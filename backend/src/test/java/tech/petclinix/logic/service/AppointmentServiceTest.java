package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.ActionEvent;
import tech.petclinix.logic.domain.AppointmentStatus;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.AppointmentNotCancellableException;
import tech.petclinix.logic.domain.exception.AppointmentOverlapException;
import tech.petclinix.logic.domain.exception.CancellationCutoffException;
import tech.petclinix.logic.domain.exception.InvalidAppointmentStatusException;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AppointmentService}.
 *
 * Repository is mocked — no database. Uses a fixed {@link Clock} well before any
 * appointment's start time in these tests, so the 2-hour cancellation cutoff does not
 * trip unless a test deliberately places "now" inside the cutoff window.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 5, 30, 8, 0);

    @Mock
    private AppointmentJpaRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Clock clock;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        appointmentService = new AppointmentService(repository, eventPublisher, clock);
    }

    private AppointmentEntity buildAppointment() {
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        return new AppointmentEntity(location, pet, startsAt, startsAt.plusMinutes(30));
    }

    /** Returns all appointments belonging to the given owner. */
    @Test
    void findAllByOwnerReturnsList() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(appointment));

        //act
        var result = appointmentService.findAllByOwner(username);

        //assert
        assertThat(result).hasSize(1);
        verify(repository).findAll(any(Specification.class));
    }

    /** Returns an empty list when the owner has no appointments. */
    @Test
    void findAllByOwnerReturnsEmptyListWhenNoAppointments() {
        //arrange
        var username = new Username("grace");
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        //act
        var result = appointmentService.findAllByOwner(username);

        //assert
        assertThat(result).isEmpty();
    }

    /** Returns all appointments belonging to the given vet. */
    @Test
    void findAllByVetReturnsList() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(appointment));

        //act
        var result = appointmentService.findAllByVet(username);

        //assert
        assertThat(result).hasSize(1);
    }

    /** Returns the appointment entity when found by vet username and id. */
    @Test
    void retrieveByVetAndIdReturnsAppointmentWhenFound() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        var result = appointmentService.retrieveByVetAndId(username, 1L);

        //assert
        assertThat(result.getVet().getUsername()).isEqualTo("vet-jack");
    }

    /** Throws NotFoundException when no appointment is found for the given vet and id. */
    @Test
    void retrieveByVetAndIdThrowsNotFoundWhenAppointmentDoesNotExist() {
        //arrange
        var username = new Username("vet-jack");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> appointmentService.retrieveByVetAndId(username, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    /** Returns the appointment entity when found by owner username and id. */
    @Test
    void retrieveByOwnerAndIdReturnsAppointmentWhenFound() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        var result = appointmentService.retrieveByOwnerAndId(username, 1L);

        //assert
        assertThat(result.getPet().getOwner().getUsername()).isEqualTo("grace");
    }

    /** Throws NotFoundException when no appointment is found for the given owner and id. */
    @Test
    void retrieveByOwnerAndIdThrowsNotFoundWhenAppointmentDoesNotExist() {
        //arrange
        var username = new Username("grace");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> appointmentService.retrieveByOwnerAndId(username, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    /** Saves a new appointment entity, publishes an APPOINTMENT_BOOKED event for the pet's owner, and returns it. */
    @Test
    void persistSavesAppointmentAndReturnsEntity() {
        //arrange
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var endsAt = startsAt.plusMinutes(30);
        var appointment = new AppointmentEntity(location, pet, startsAt, endsAt);

        when(repository.findOverlappingForUpdate(vet, startsAt, endsAt)).thenReturn(List.of());
        when(repository.save(any(AppointmentEntity.class))).thenReturn(appointment);

        //act
        var result = appointmentService.persist(pet, location, startsAt, endsAt);

        //assert
        assertThat(result.getStartAt()).isEqualTo(startsAt);
        assertThat(result.getEndsAt()).isEqualTo(endsAt);
        verify(repository).save(any(AppointmentEntity.class));
        verify(eventPublisher).publishEvent(new ActionEvent(new Username("grace"), "APPOINTMENT_BOOKED"));
    }

    /** Throws AppointmentOverlapException and does not save when the vet already has an overlapping appointment. */
    @Test
    void persistThrowsAppointmentOverlapExceptionWhenOverlapExists() {
        //arrange
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var endsAt = startsAt.plusMinutes(30);
        var conflicting = new AppointmentEntity(location, pet, startsAt, endsAt);

        when(repository.findOverlappingForUpdate(vet, startsAt, endsAt)).thenReturn(List.of(conflicting));

        //act + assert
        assertThatThrownBy(() -> appointmentService.persist(pet, location, startsAt, endsAt))
                .isInstanceOf(AppointmentOverlapException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Translates a DB-level unique constraint violation on save into AppointmentOverlapException. */
    @Test
    void persistTranslatesDataIntegrityViolationIntoAppointmentOverlapException() {
        //arrange
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var endsAt = startsAt.plusMinutes(30);

        when(repository.findOverlappingForUpdate(vet, startsAt, endsAt)).thenReturn(List.of());
        when(repository.save(any(AppointmentEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        //act + assert
        assertThatThrownBy(() -> appointmentService.persist(pet, location, startsAt, endsAt))
                .isInstanceOf(AppointmentOverlapException.class);
    }

    /** Transitions the appointment to CANCELLED and saves it (no delete) when cancelling a BOOKED appointment by owner. */
    @Test
    void cancelByOwnerCancelsBookedAppointment() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        appointmentService.cancelByOwner(username, 1L);

        //assert
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(repository).save(appointment);
        verify(repository, never()).delete(any(AppointmentEntity.class));
        verify(eventPublisher).publishEvent(new ActionEvent(username, "APPOINTMENT_CANCELLED"));
    }

    /** Transitions the appointment to CANCELLED when cancelling a CONFIRMED appointment by owner. */
    @Test
    void cancelByOwnerCancelsConfirmedAppointment() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        appointment.confirm();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        appointmentService.cancelByOwner(username, 1L);

        //assert
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(repository).save(appointment);
    }

    /** Throws NotFoundException when cancelling by owner and appointment does not exist. */
    @Test
    void cancelByOwnerThrowsNotFoundWhenAppointmentDoesNotExist() {
        //arrange
        var username = new Username("grace");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByOwner(username, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    /** Throws AppointmentNotCancellableException when the owner cancels an already-cancelled appointment. */
    @Test
    void cancelByOwnerThrowsNotCancellableWhenAppointmentAlreadyCancelled() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        appointment.cancel();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByOwner(username, 1L))
                .isInstanceOf(AppointmentNotCancellableException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Throws AppointmentNotCancellableException when the owner cancels a completed appointment. */
    @Test
    void cancelByOwnerThrowsNotCancellableWhenAppointmentCompleted() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        appointment.confirm();
        appointment.complete();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByOwner(username, 1L))
                .isInstanceOf(AppointmentNotCancellableException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Throws AppointmentNotCancellableException when the owner cancels a no-show appointment. */
    @Test
    void cancelByOwnerThrowsNotCancellableWhenAppointmentNoShow() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        appointment.confirm();
        appointment.markNoShow();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByOwner(username, 1L))
                .isInstanceOf(AppointmentNotCancellableException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Throws CancellationCutoffException when cancelling inside the 2-hour cutoff window. */
    @Test
    void cancelByOwnerThrowsCancellationCutoffExceptionWithinCutoffWindow() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = NOW.plusHours(1); // inside the 2-hour cutoff
        var appointment = new AppointmentEntity(location, pet, startsAt, startsAt.plusMinutes(30));
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByOwner(username, 1L))
                .isInstanceOf(CancellationCutoffException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Allows cancellation exactly outside the cutoff window (more than 2 hours before start). */
    @Test
    void cancelByOwnerSucceedsJustOutsideCutoffWindow() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = NOW.plusHours(2).plusMinutes(1); // just outside the 2-hour cutoff
        var appointment = new AppointmentEntity(location, pet, startsAt, startsAt.plusMinutes(30));
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        appointmentService.cancelByOwner(username, 1L);

        //assert
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    /** Transitions the appointment to CANCELLED and saves it (no delete) when cancelling by vet. */
    @Test
    void cancelByVetCancelsAppointment() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        appointmentService.cancelByVet(username, 1L);

        //assert
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(repository).save(appointment);
        verify(repository, never()).delete(any(AppointmentEntity.class));
        verify(eventPublisher).publishEvent(new ActionEvent(username, "APPOINTMENT_CANCELLED"));
    }

    /** Throws NotFoundException when cancelling by vet and appointment does not exist. */
    @Test
    void cancelByVetThrowsNotFoundWhenAppointmentDoesNotExist() {
        //arrange
        var username = new Username("vet-jack");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByVet(username, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    /** Throws AppointmentNotCancellableException when the vet cancels an already-cancelled appointment. */
    @Test
    void cancelByVetThrowsNotCancellableWhenAppointmentAlreadyCancelled() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        appointment.cancel();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByVet(username, 1L))
                .isInstanceOf(AppointmentNotCancellableException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Transitions a BOOKED appointment to CONFIRMED and publishes an APPOINTMENT_CONFIRMED event. */
    @Test
    void confirmByVetConfirmsBookedAppointment() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        appointmentService.confirmByVet(username, 1L);

        //assert
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(repository).save(appointment);
        verify(eventPublisher).publishEvent(new ActionEvent(username, "APPOINTMENT_CONFIRMED"));
    }

    /** Throws InvalidAppointmentStatusException when confirming an appointment that is not BOOKED. */
    @Test
    void confirmByVetThrowsInvalidAppointmentStatusExceptionWhenNotBooked() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        appointment.confirm();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.confirmByVet(username, 1L))
                .isInstanceOf(InvalidAppointmentStatusException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Transitions a CONFIRMED appointment to NO_SHOW and publishes an APPOINTMENT_NO_SHOW event. */
    @Test
    void markNoShowByVetMarksConfirmedAppointmentAsNoShow() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        appointment.confirm();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        appointmentService.markNoShowByVet(username, 1L);

        //assert
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        verify(repository).save(appointment);
        verify(eventPublisher).publishEvent(new ActionEvent(username, "APPOINTMENT_NO_SHOW"));
    }

    /** Throws InvalidAppointmentStatusException when marking no-show on an appointment that is not CONFIRMED. */
    @Test
    void markNoShowByVetThrowsInvalidAppointmentStatusExceptionWhenNotConfirmed() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.markNoShowByVet(username, 1L))
                .isInstanceOf(InvalidAppointmentStatusException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Transitions a CONFIRMED appointment to COMPLETED and publishes an APPOINTMENT_COMPLETED event. */
    @Test
    void completeByVetCompletesConfirmedAppointment() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        appointment.confirm();

        //act
        appointmentService.completeByVet(username, appointment);

        //assert
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(repository).save(appointment);
        verify(eventPublisher).publishEvent(new ActionEvent(username, "APPOINTMENT_COMPLETED"));
    }

    /** Throws InvalidAppointmentStatusException when completing an appointment that is not CONFIRMED. */
    @Test
    void completeByVetThrowsInvalidAppointmentStatusExceptionWhenNotConfirmed() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();

        //act + assert
        assertThatThrownBy(() -> appointmentService.completeByVet(username, appointment))
                .isInstanceOf(InvalidAppointmentStatusException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }

    /** Books the new slot and cancels the old appointment, publishing an APPOINTMENT_RESCHEDULED event. */
    @Test
    void rescheduleBooksNewSlotAndCancelsOldAppointment() {
        //arrange
        var username = new Username("grace");
        var oldAppointment = buildAppointment();
        var newStartsAt = LocalDateTime.of(2025, 6, 2, 10, 0);
        var newEndsAt = newStartsAt.plusMinutes(30);
        var newAppointment = new AppointmentEntity(oldAppointment.getLocation(), oldAppointment.getPet(), newStartsAt, newEndsAt);

        when(repository.findOverlappingForUpdate(oldAppointment.getVet(), newStartsAt, newEndsAt)).thenReturn(List.of());
        when(repository.save(any(AppointmentEntity.class))).thenReturn(newAppointment);

        //act
        var result = appointmentService.reschedule(username, oldAppointment, newStartsAt, newEndsAt);

        //assert
        assertThat(result.getStartAt()).isEqualTo(newStartsAt);
        assertThat(oldAppointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(repository).save(oldAppointment);
        verify(eventPublisher).publishEvent(new ActionEvent(username, "APPOINTMENT_RESCHEDULED"));
    }

    /** Leaves the old appointment untouched when the new slot overlaps another appointment. */
    @Test
    void rescheduleLeavesOldAppointmentUntouchedWhenNewSlotOverlaps() {
        //arrange
        var username = new Username("grace");
        var oldAppointment = buildAppointment();
        var newStartsAt = LocalDateTime.of(2025, 6, 2, 10, 0);
        var newEndsAt = newStartsAt.plusMinutes(30);
        var conflicting = new AppointmentEntity(oldAppointment.getLocation(), oldAppointment.getPet(), newStartsAt, newEndsAt);

        when(repository.findOverlappingForUpdate(oldAppointment.getVet(), newStartsAt, newEndsAt)).thenReturn(List.of(conflicting));

        //act + assert
        assertThatThrownBy(() -> appointmentService.reschedule(username, oldAppointment, newStartsAt, newEndsAt))
                .isInstanceOf(AppointmentOverlapException.class);
        assertThat(oldAppointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        verify(repository, never()).save(oldAppointment);
    }

    /** Rejects rescheduling when the old appointment is within the cancellation cutoff window, without attempting to book the new slot. */
    @Test
    void rescheduleThrowsCancellationCutoffExceptionWhenOldAppointmentWithinCutoff() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var pet = new PetEntity("Fluffy", owner);
        var oldStartsAt = NOW.plusHours(1); // inside the 2-hour cutoff
        var oldAppointment = new AppointmentEntity(location, pet, oldStartsAt, oldStartsAt.plusMinutes(30));
        var newStartsAt = LocalDateTime.of(2025, 6, 2, 10, 0);
        var newEndsAt = newStartsAt.plusMinutes(30);

        //act + assert
        assertThatThrownBy(() -> appointmentService.reschedule(username, oldAppointment, newStartsAt, newEndsAt))
                .isInstanceOf(CancellationCutoffException.class);
        verify(repository, never()).findOverlappingForUpdate(any(), any(), any());
        verify(repository, never()).save(any(AppointmentEntity.class));
    }
}
