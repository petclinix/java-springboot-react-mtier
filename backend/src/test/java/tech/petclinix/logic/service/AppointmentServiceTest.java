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
import tech.petclinix.logic.domain.exception.AppointmentAlreadyCancelledException;
import tech.petclinix.logic.domain.exception.AppointmentOverlapException;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;

import java.time.LocalDateTime;
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
 * Repository is mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentJpaRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(repository, eventPublisher);
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

    /** Transitions the appointment to CANCELLED and saves it (no delete) when cancelling by owner. */
    @Test
    void cancelByOwnerCancelsAppointment() {
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

    /** Throws AppointmentAlreadyCancelledException when the owner cancels an already-cancelled appointment. */
    @Test
    void cancelByOwnerThrowsAlreadyCancelledWhenAppointmentAlreadyCancelled() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        appointment.cancel();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByOwner(username, 1L))
                .isInstanceOf(AppointmentAlreadyCancelledException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
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

    /** Throws AppointmentAlreadyCancelledException when the vet cancels an already-cancelled appointment. */
    @Test
    void cancelByVetThrowsAlreadyCancelledWhenAppointmentAlreadyCancelled() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        appointment.cancel();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act + assert
        assertThatThrownBy(() -> appointmentService.cancelByVet(username, 1L))
                .isInstanceOf(AppointmentAlreadyCancelledException.class);
        verify(repository, never()).save(any(AppointmentEntity.class));
    }
}
