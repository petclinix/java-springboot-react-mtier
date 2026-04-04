package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.AppointmentEntity;
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

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(repository);
    }

    private AppointmentEntity buildAppointment() {
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        return new AppointmentEntity(vet, pet, LocalDateTime.of(2025, 6, 1, 10, 0));
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

    /** Saves a new appointment entity and returns it. */
    @Test
    void persistSavesAppointmentAndReturnsEntity() {
        //arrange
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var appointment = new AppointmentEntity(vet, pet, startsAt);

        when(repository.save(any(AppointmentEntity.class))).thenReturn(appointment);

        //act
        var result = appointmentService.persist(pet, vet, startsAt);

        //assert
        assertThat(result.getStartAt()).isEqualTo(startsAt);
        verify(repository).save(any(AppointmentEntity.class));
    }

    /** Deletes the appointment when cancelling by owner. */
    @Test
    void cancelByOwnerDeletesAppointment() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        appointmentService.cancelByOwner(username, 1L);

        //assert
        verify(repository).delete(appointment);
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

    /** Deletes the appointment when cancelling by vet. */
    @Test
    void cancelByVetDeletesAppointment() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        //act
        appointmentService.cancelByVet(username, 1L);

        //assert
        verify(repository).delete(appointment);
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
}
