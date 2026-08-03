package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.AppointmentAlreadyCancelledException;
import tech.petclinix.logic.domain.exception.AppointmentOverlapException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AppointmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentJpaRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentService(AppointmentJpaRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
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

    /* default */ AppointmentEntity persist(PetEntity pet, LocationEntity location, LocalDateTime startAt, LocalDateTime endsAt) {
        VetEntity vet = location.getVet();
        List<AppointmentEntity> overlapping = repository.findOverlappingForUpdate(vet, startAt, endsAt);
        if (!overlapping.isEmpty()) {
            throw new AppointmentOverlapException(vet.getId(), startAt, endsAt);
        }
        var appointment = new AppointmentEntity(location, pet, startAt, endsAt);
        AppointmentEntity saved;
        try {
            saved = repository.save(appointment);
        } catch (DataIntegrityViolationException e) {
            // Two transactions raced past the pessimistic-lock check before either committed;
            // the DB-level unique constraint (vet_id, start_at) is the final backstop.
            throw new AppointmentOverlapException(vet.getId(), startAt, endsAt);
        }
        eventPublisher.publishEvent(new ActionEvent(new Username(pet.getOwner().getUsername()), "APPOINTMENT_BOOKED"));
        LOGGER.info("Appointment {} booked: pet {} with vet {} at {}", saved.getId(), pet.getId(), vet.getId(), startAt);
        return saved;
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

    private void cancelBySpec(Long appointmentId, Specification<AppointmentEntity> spec, Supplier<String> notFoundContext) {
        var appointment = retrieveByIdAndSpec(appointmentId, spec, notFoundContext);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new AppointmentAlreadyCancelledException(appointmentId);
        }
        appointment.cancel();
        repository.save(appointment);
    }

    private AppointmentEntity retrieveByIdAndSpec(Long appointmentId, Specification<AppointmentEntity> spec, Supplier<String> notFoundContext) {
        return repository.findOne(Specifications.byId(appointmentId).and(spec))
                .orElseThrow(() -> new NotFoundException("Appointment not found: %s".formatted(notFoundContext.get())));
    }

}
