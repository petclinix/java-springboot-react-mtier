package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.Appointment;
import tech.petclinix.logic.domain.AppointmentData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link OwnerAppointmentService}.
 *
 * All data services are mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class OwnerAppointmentServiceTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private PetService petService;

    @Mock
    private VetService vetService;

    private OwnerAppointmentService ownerAppointmentService;

    @BeforeEach
    void setUp() {
        ownerAppointmentService = new OwnerAppointmentService(appointmentService, petService, vetService);
    }

    private AppointmentEntity buildAppointment() {
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        return new AppointmentEntity(vet, pet, LocalDateTime.of(2025, 6, 1, 10, 0));
    }

    /** Returns all appointments for the owner mapped to domain records. */
    @Test
    void findAllByOwnerReturnsMappedAppointments() {
        //arrange
        var username = new Username("grace");
        var appointment = buildAppointment();
        when(appointmentService.findAllByOwner(username)).thenReturn(List.of(appointment));

        //act
        List<Appointment> result = ownerAppointmentService.findAllByOwner(username);

        //assert
        assertThat(result).hasSize(1);
        verify(appointmentService).findAllByOwner(username);
    }

    /** Returns an empty list when the owner has no appointments. */
    @Test
    void findAllByOwnerReturnsEmptyListWhenNoAppointments() {
        //arrange
        var username = new Username("grace");
        when(appointmentService.findAllByOwner(username)).thenReturn(List.of());

        //act
        List<Appointment> result = ownerAppointmentService.findAllByOwner(username);

        //assert
        assertThat(result).isEmpty();
    }

    /** Persists an appointment using pet and vet resolved from data services. */
    @Test
    void persistCreatesAppointmentAndReturnsDomainRecord() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var appointment = new AppointmentEntity(vet, pet, startsAt);

        AppointmentData appointmentData = new AppointmentData() {
            public Long vetId() { return 1L; }
            public Long petId() { return 2L; }
            public LocalDateTime startsAt() { return startsAt; }
        };

        when(petService.retrieveByOwnerAndId(username, 2L)).thenReturn(pet);
        when(vetService.retrieveById(1L)).thenReturn(vet);
        when(appointmentService.persist(pet, vet, startsAt)).thenReturn(appointment);

        //act
        Appointment result = ownerAppointmentService.persist(username, appointmentData);

        //assert
        assertThat(result.startsAt()).isEqualTo(startsAt);
        verify(petService).retrieveByOwnerAndId(username, 2L);
        verify(vetService).retrieveById(1L);
        verify(appointmentService).persist(pet, vet, startsAt);
    }

    /** Delegates cancellation to the appointment service. */
    @Test
    void cancelByOwnerDelegatestoAppointmentService() {
        //arrange
        var username = new Username("grace");

        //act
        ownerAppointmentService.cancelByOwner(username, 1L);

        //assert
        verify(appointmentService).cancelByOwner(username, 1L);
    }
}
