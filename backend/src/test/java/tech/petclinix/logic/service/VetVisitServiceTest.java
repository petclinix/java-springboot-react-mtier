package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.VetVisit;
import tech.petclinix.logic.domain.VetVisitData;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VisitEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link VetVisitService}.
 *
 * AppointmentService and VisitService are mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class VetVisitServiceTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private VisitService visitService;

    private VetVisitService vetVisitService;

    @BeforeEach
    void setUp() {
        vetVisitService = new VetVisitService(appointmentService, visitService);
    }

    private AppointmentEntity buildAppointment() {
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        return new AppointmentEntity(vet, pet, LocalDateTime.of(2025, 6, 1, 10, 0));
    }

    /** Returns the visit for the given vet and appointment id, mapped to a domain record. */
    @Test
    void retrieveByVetAndIdReturnsMappedVetVisit() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        var visitEntity = new VisitEntity(appointment);
        visitEntity.setVetSummary("All clear");
        visitEntity.setOwnerSummary("Pet was lively");
        visitEntity.setVaccination("Rabies");

        when(appointmentService.retrieveByVetAndId(username, 1L)).thenReturn(appointment);
        when(visitService.findByAppointment(appointment)).thenReturn(Optional.of(visitEntity));

        //act
        VetVisit result = vetVisitService.retrieveByVetAndId(username, 1L);

        //assert
        assertThat(result.vetSummary()).isEqualTo("All clear");
        assertThat(result.ownerSummary()).isEqualTo("Pet was lively");
        assertThat(result.vaccination()).isEqualTo("Rabies");
        verify(appointmentService).retrieveByVetAndId(username, 1L);
        verify(visitService).findByAppointment(appointment);
    }

    /** Returns an empty VetVisit when no visit record exists yet for the appointment. */
    @Test
    void retrieveByVetAndIdReturnsEmptyVetVisitWhenNoVisitExists() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();

        when(appointmentService.retrieveByVetAndId(username, 1L)).thenReturn(appointment);
        when(visitService.findByAppointment(appointment)).thenReturn(Optional.empty());

        //act
        VetVisit result = vetVisitService.retrieveByVetAndId(username, 1L);

        //assert
        assertThat(result.id()).isNull();
        assertThat(result.vetSummary()).isNull();
        assertThat(result.ownerSummary()).isNull();
        assertThat(result.vaccination()).isNull();
    }

    /** Persists a visit for the given vet and appointment id and returns the mapped domain record. */
    @Test
    void persistCreatesOrUpdatesVisitAndReturnsDomainRecord() {
        //arrange
        var username = new Username("vet-jack");
        var appointment = buildAppointment();
        var visitEntity = new VisitEntity(appointment);
        visitEntity.setVetSummary("Healthy");
        visitEntity.setOwnerSummary("No issues");
        visitEntity.setVaccination("DHPP");

        VetVisitData visitData = new VetVisitData() {
            public String vetSummary() { return "Healthy"; }
            public String ownerSummary() { return "No issues"; }
            public String vaccination() { return "DHPP"; }
        };

        when(appointmentService.retrieveByVetAndId(username, 1L)).thenReturn(appointment);
        when(visitService.persist(eq(appointment), eq("Healthy"), eq("No issues"), eq("DHPP")))
                .thenReturn(visitEntity);

        //act
        VetVisit result = vetVisitService.persist(username, 1L, visitData);

        //assert
        assertThat(result.vetSummary()).isEqualTo("Healthy");
        assertThat(result.vaccination()).isEqualTo("DHPP");
        verify(appointmentService).retrieveByVetAndId(username, 1L);
        verify(visitService).persist(appointment, "Healthy", "No issues", "DHPP");
    }
}
