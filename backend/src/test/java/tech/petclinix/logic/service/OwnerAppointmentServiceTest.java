package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.Appointment;
import tech.petclinix.logic.domain.AppointmentData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.LocationClosedAtRequestedTimeException;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.OpeningPeriodEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    private LocationService locationService;

    private OwnerAppointmentService ownerAppointmentService;

    @BeforeEach
    void setUp() {
        ownerAppointmentService = new OwnerAppointmentService(appointmentService, petService, locationService);
    }

    private AppointmentEntity buildAppointment() {
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        return new AppointmentEntity(location, pet, startsAt, startsAt.plusMinutes(30));
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

    /** Persists an appointment using pet and location resolved from data services, computing endsAt from the default duration. */
    @Test
    void persistCreatesAppointmentAndReturnsDomainRecord() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var endsAt = startsAt.plusMinutes(30);

        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var period = new OpeningPeriodEntity(location, startsAt.getDayOfWeek().getValue(),
                startsAt.toLocalTime().minusHours(1), startsAt.toLocalTime().plusHours(1), 0);
        location.addWeeklyPeriod(period);

        var appointment = new AppointmentEntity(location, pet, startsAt, endsAt);

        AppointmentData appointmentData = new AppointmentData() {
            public Long locationId() { return 5L; }
            public Long petId() { return 2L; }
            public LocalDateTime startsAt() { return startsAt; }
        };

        when(petService.retrieveByOwnerAndId(username, 2L)).thenReturn(pet);
        when(locationService.retrieveById(5L)).thenReturn(location);
        when(appointmentService.persist(pet, location, startsAt, endsAt)).thenReturn(appointment);

        //act
        Appointment result = ownerAppointmentService.persist(username, appointmentData);

        //assert
        assertThat(result.startsAt()).isEqualTo(startsAt);
        assertThat(result.endsAt()).isEqualTo(endsAt);
        verify(petService).retrieveByOwnerAndId(username, 2L);
        verify(locationService).retrieveById(5L);
        verify(appointmentService).persist(pet, location, startsAt, endsAt);
    }

    /** Throws LocationClosedAtRequestedTimeException when the location is not open at the requested time. */
    @Test
    void persistThrowsLocationClosedAtRequestedTimeExceptionWhenLocationIsClosed() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var startsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var location = new LocationEntity(vet, "Clinic North", "UTC");

        AppointmentData appointmentData = new AppointmentData() {
            public Long locationId() { return 5L; }
            public Long petId() { return 2L; }
            public LocalDateTime startsAt() { return startsAt; }
        };

        when(petService.retrieveByOwnerAndId(username, 2L)).thenReturn(pet);
        when(locationService.retrieveById(5L)).thenReturn(location);

        //act + assert
        assertThatThrownBy(() -> ownerAppointmentService.persist(username, appointmentData))
                .isInstanceOf(LocationClosedAtRequestedTimeException.class);
        verify(appointmentService, never()).persist(any(), any(), any(), any());
    }

}
