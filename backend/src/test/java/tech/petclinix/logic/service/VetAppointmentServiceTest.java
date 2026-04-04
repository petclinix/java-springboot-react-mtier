package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.VetAppointment;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link VetAppointmentService}.
 *
 * AppointmentService is mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class VetAppointmentServiceTest {

    @Mock
    private AppointmentService appointmentService;

    private VetAppointmentService vetAppointmentService;

    @BeforeEach
    void setUp() {
        vetAppointmentService = new VetAppointmentService(appointmentService);
    }

    private AppointmentEntity buildAppointment() {
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        return new AppointmentEntity(vet, pet, LocalDateTime.of(2025, 6, 1, 10, 0));
    }

    /** Returns all appointments for the vet mapped to domain records. */
    @Test
    void findAllByVetReturnsMappedVetAppointments() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        when(appointmentService.findAllByVet(username)).thenReturn(List.of(appointment));

        //act
        List<VetAppointment> result = vetAppointmentService.findAllByVet(username);

        //assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).petName()).isEqualTo("Fluffy");
        assertThat(result.get(0).ownerUsername()).isEqualTo("grace");
        verify(appointmentService).findAllByVet(username);
    }

    /** Returns an empty list when the vet has no appointments. */
    @Test
    void findAllByVetReturnsEmptyListWhenNoAppointments() {
        //arrange
        var username = new Username("vet-jack");
        when(appointmentService.findAllByVet(username)).thenReturn(List.of());

        //act
        List<VetAppointment> result = vetAppointmentService.findAllByVet(username);

        //assert
        assertThat(result).isEmpty();
    }

    /** Delegates cancellation to the appointment service. */
    @Test
    void cancelByVetDelegatesToAppointmentService() {
        //arrange
        var username = new Username("vet-jack");

        //act
        vetAppointmentService.cancelByVet(username, 1L);

        //assert
        verify(appointmentService).cancelByVet(username, 1L);
    }
}
