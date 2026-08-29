package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.AppointmentNotCancellableException;
import tech.petclinix.logic.domain.exception.AppointmentOverlapException;
import tech.petclinix.logic.domain.exception.CancellationCutoffException;
import tech.petclinix.logic.domain.exception.InvalidAppointmentStatusException;
import tech.petclinix.logic.domain.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tech.petclinix.logic.domain.ActionEvent;
import tech.petclinix.logic.domain.AppointmentStatus;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;

import tech.petclinix.persistence.jpa.AppointmentJpaRepository.Specifications;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AppointmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentService.class);

    /** An appointment can no longer be cancelled (or rescheduled) inside this window before its start time. */
    static final long CANCELLATION_CUTOFF_HOURS = 2;

    private final AppointmentJpaRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AppointmentService(AppointmentJpaRepository repository, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /* default */ List<AppointmentEntity> findAllByOwner(Username ownerUsername) {
        return repository.findAll(Specifications.byOwnerUsername(ownerUsername).and(Specifications.active()));
    }

    /* default */ List<AppointmentEntity> findAllByVet(Username vetUsername) {
        return repository.findAll(Specifications.byVetUsername(vetUsername).and(Specifications.active()));
    }

    /* default */ AppointmentEntity retrieveByVetAndId(Username vetUsername, Long appointmentId) {
        return retrieveByIdAndSpec(appointmentId, Specifications.byVetUsername(vetUsername),
                () -> "vet %s, id %d".formatted(vetUsername.value(), appointmentId)
        );
    }

    /* default */ AppointmentEntity retrieveByOwnerAndId(Username ownerUsername, Long appointmentId) {
        return retrieveByIdAndSpec(appointmentId, Specifications.byOwnerUsername(ownerUsername),
                () -> "owner %s, id %d".formatted(ownerUsername.value(), appointmentId)
        );
    }

    /* default */ AppointmentEntity persist(PetEntity pet, LocationEntity location, LocalDateTime startAt, LocalDateTime endsAt) {
        AppointmentEntity saved = bookSlot(pet, location, startAt, endsAt);
        eventPublisher.publishEvent(new ActionEvent(new Username(pet.getOwner().getUsername()), "APPOINTMENT_BOOKED"));
        LOGGER.info("Appointment {} booked: pet {} with vet {} at {}", saved.getId(), pet.getId(), location.getVet().getId(), startAt);
        return saved;
    }

    private AppointmentEntity bookSlot(PetEntity pet, LocationEntity location, LocalDateTime startAt, LocalDateTime endsAt) {
        VetEntity vet = location.getVet();
        List<AppointmentEntity> overlapping = repository.findOverlappingForUpdate(vet, startAt, endsAt);
        if (!overlapping.isEmpty()) {
            throw new AppointmentOverlapException(vet.getId(), startAt, endsAt);
        }
        var appointment = new AppointmentEntity(location, pet, startAt, endsAt);
        try {
            return repository.save(appointment);
        } catch (DataIntegrityViolationException e) {
            // Two transactions raced past the pessimistic-lock check before either committed;
            // the DB-level unique constraint (vet_id, start_at) is the final backstop.
            throw new AppointmentOverlapException(vet.getId(), startAt, endsAt);
        }
    }

    /* default */ void cancelByOwner(Username ownerUsername, Long appointmentId) {
        cancelBySpec(
                appointmentId, Specifications.byOwnerUsername(ownerUsername),
                () -> "owner %s, id %d".formatted(ownerUsername.value(), appointmentId)
        );
        eventPublisher.publishEvent(new ActionEvent(ownerUsername, "APPOINTMENT_CANCELLED"));
        LOGGER.info("Appointment {} cancelled by owner {}", appointmentId, ownerUsername.value());
    }

    /* default */ void cancelByVet(Username vetUsername, Long appointmentId) {
        cancelBySpec(
                appointmentId, Specifications.byVetUsername(vetUsername),
                () -> "vet %s, id %d".formatted(vetUsername.value(), appointmentId)
        );
        eventPublisher.publishEvent(new ActionEvent(vetUsername, "APPOINTMENT_CANCELLED"));
        LOGGER.info("Appointment {} cancelled by vet {}", appointmentId, vetUsername.value());
    }

    /* default */ void confirmByVet(Username vetUsername, Long appointmentId) {
        var appointment = retrieveByVetAndId(vetUsername, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new InvalidAppointmentStatusException(appointmentId, AppointmentStatus.BOOKED, appointment.getStatus());
        }
        appointment.confirm();
        repository.save(appointment);
        eventPublisher.publishEvent(new ActionEvent(vetUsername, "APPOINTMENT_CONFIRMED"));
        LOGGER.info("Appointment {} confirmed by vet {}", appointmentId, vetUsername.value());
    }

    /* default */ void markNoShowByVet(Username vetUsername, Long appointmentId) {
        var appointment = retrieveByVetAndId(vetUsername, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidAppointmentStatusException(appointmentId, AppointmentStatus.CONFIRMED, appointment.getStatus());
        }
        appointment.markNoShow();
        repository.save(appointment);
        eventPublisher.publishEvent(new ActionEvent(vetUsername, "APPOINTMENT_NO_SHOW"));
        LOGGER.info("Appointment {} marked no-show by vet {}", appointmentId, vetUsername.value());
    }

    /**
     * Transitions the appointment (already resolved and owned by the caller — see
     * {@link #retrieveByVetAndId}) from CONFIRMED to COMPLETED. Called by {@code VetVisitService}
     * as part of recording a visit, in the same transaction as the visit write.
     */
    /* default */ void completeByVet(Username vetUsername, AppointmentEntity appointment) {
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidAppointmentStatusException(appointment.getId(), AppointmentStatus.CONFIRMED, appointment.getStatus());
        }
        appointment.complete();
        repository.save(appointment);
        eventPublisher.publishEvent(new ActionEvent(vetUsername, "APPOINTMENT_COMPLETED"));
        LOGGER.info("Appointment {} completed by vet {}", appointment.getId(), vetUsername.value());
    }

    /**
     * Reschedules an already-resolved appointment (ownership already checked by the caller —
     * see {@link #retrieveByOwnerAndId}) to a new slot: the old slot must still be cancellable
     * (right status, outside the cutoff), and the new slot goes through the same overlap /
     * concurrency booking checks as a fresh booking. If the new slot cannot be booked, the old
     * appointment is left completely untouched — cancellation only happens after the new
     * booking has succeeded.
     */
    /* default */ AppointmentEntity reschedule(Username ownerUsername, AppointmentEntity oldAppointment, LocalDateTime newStartsAt, LocalDateTime newEndsAt) {
        assertCancellable(oldAppointment);
        AppointmentEntity newAppointment = persist(oldAppointment.getPet(), oldAppointment.getLocation(), newStartsAt, newEndsAt);
        oldAppointment.cancel();
        repository.save(oldAppointment);
        eventPublisher.publishEvent(new ActionEvent(ownerUsername, "APPOINTMENT_RESCHEDULED"));
        LOGGER.info("Appointment {} rescheduled by owner {} to {} (new appointment {})",
                oldAppointment.getId(), ownerUsername.value(), newStartsAt, newAppointment.getId());
        return newAppointment;
    }

    private void cancelBySpec(Long appointmentId, Specification<AppointmentEntity> spec, Supplier<String> notFoundContext) {
        var appointment = retrieveByIdAndSpec(appointmentId, spec, notFoundContext);
        assertCancellable(appointment);
        appointment.cancel();
        repository.save(appointment);
    }

    /* default */ void assertCancellable(AppointmentEntity appointment) {
        if (appointment.getStatus() != AppointmentStatus.BOOKED && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppointmentNotCancellableException(appointment.getId());
        }
        LocalDateTime cutoff = appointment.getStartAt().minusHours(CANCELLATION_CUTOFF_HOURS);
        if (!LocalDateTime.now(clock).isBefore(cutoff)) {
            throw new CancellationCutoffException(appointment.getId(), CANCELLATION_CUTOFF_HOURS);
        }
    }

    private AppointmentEntity retrieveByIdAndSpec(Long appointmentId, Specification<AppointmentEntity> spec, Supplier<String> notFoundContext) {
        return repository.findOne(Specifications.byId(appointmentId).and(spec))
                .orElseThrow(() -> new NotFoundException("Appointment not found: %s".formatted(notFoundContext.get())));
    }

}
