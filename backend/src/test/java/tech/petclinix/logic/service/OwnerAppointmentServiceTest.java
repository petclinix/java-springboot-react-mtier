package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.Appointment;
import tech.petclinix.logic.domain.AppointmentData;
import tech.petclinix.logic.domain.RescheduleData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.CancellationCutoffException;
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

    private LocationEntity locationOpenAllDay(VetEntity vet, LocalDateTime aroundTime) {
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var period = new OpeningPeriodEntity(location, aroundTime.getDayOfWeek().getValue(),
                aroundTime.toLocalTime().minusHours(3), aroundTime.toLocalTime().plusHours(3), 0);
        location.addWeeklyPeriod(period);
        return location;
    }

    /** Reschedules by delegating to AppointmentService.reschedule, once the new slot is confirmed open, and returns the mapped new appointment. */
    @Test
    void rescheduleReturnsMappedNewAppointmentOnSuccess() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var oldStartsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var location = locationOpenAllDay(vet, oldStartsAt);
        var oldAppointment = new AppointmentEntity(location, pet, oldStartsAt, oldStartsAt.plusMinutes(30));

        var newStartsAt = LocalDateTime.of(2025, 6, 1, 11, 0);
        var newEndsAt = newStartsAt.plusMinutes(30);
        var newAppointment = new AppointmentEntity(location, pet, newStartsAt, newEndsAt);

        RescheduleData rescheduleData = () -> newStartsAt;

        when(appointmentService.retrieveByOwnerAndId(username, 1L)).thenReturn(oldAppointment);
        when(appointmentService.reschedule(username, oldAppointment, newStartsAt, newEndsAt)).thenReturn(newAppointment);

        //act
        Appointment result = ownerAppointmentService.reschedule(username, 1L, rescheduleData);

        //assert
        assertThat(result.startsAt()).isEqualTo(newStartsAt);
        verify(appointmentService).reschedule(username, oldAppointment, newStartsAt, newEndsAt);
    }

    /** Throws LocationClosedAtRequestedTimeException without touching AppointmentService.reschedule when the new slot is outside opening hours. */
    @Test
    void rescheduleThrowsLocationClosedAtRequestedTimeExceptionWhenNewSlotIsClosed() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var oldStartsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var location = new LocationEntity(vet, "Clinic North", "UTC"); // no opening periods -> always closed
        var oldAppointment = new AppointmentEntity(location, pet, oldStartsAt, oldStartsAt.plusMinutes(30));

        var newStartsAt = LocalDateTime.of(2025, 6, 1, 11, 0);
        RescheduleData rescheduleData = () -> newStartsAt;

        when(appointmentService.retrieveByOwnerAndId(username, 1L)).thenReturn(oldAppointment);

        //act + assert
        assertThatThrownBy(() -> ownerAppointmentService.reschedule(username, 1L, rescheduleData))
                .isInstanceOf(LocationClosedAtRequestedTimeException.class);
        verify(appointmentService, never()).reschedule(any(), any(), any(), any());
    }

    /** Propagates CancellationCutoffException from AppointmentService.reschedule when the old appointment is inside the cutoff window, leaving the old appointment untouched by this layer. */
    @Test
    void reschedulePropagatesCancellationCutoffExceptionFromAppointmentService() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var oldStartsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var location = locationOpenAllDay(vet, oldStartsAt);
        var oldAppointment = new AppointmentEntity(location, pet, oldStartsAt, oldStartsAt.plusMinutes(30));

        var newStartsAt = LocalDateTime.of(2025, 6, 1, 11, 0);
        var newEndsAt = newStartsAt.plusMinutes(30);
        RescheduleData rescheduleData = () -> newStartsAt;

        when(appointmentService.retrieveByOwnerAndId(username, 1L)).thenReturn(oldAppointment);
        when(appointmentService.reschedule(username, oldAppointment, newStartsAt, newEndsAt))
                .thenThrow(new CancellationCutoffException(1L, 2));

        //act + assert
        assertThatThrownBy(() -> ownerAppointmentService.reschedule(username, 1L, rescheduleData))
                .isInstanceOf(CancellationCutoffException.class);
        assertThat(oldAppointment.getStatus()).isEqualTo(tech.petclinix.logic.domain.AppointmentStatus.BOOKED);
    }

    /** Propagates AppointmentOverlapException from AppointmentService.reschedule when the new slot conflicts with another appointment, leaving the old appointment untouched. */
    @Test
    void reschedulePropagatesAppointmentOverlapExceptionAndLeavesOldAppointmentIntact() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var oldStartsAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        var location = locationOpenAllDay(vet, oldStartsAt);
        var oldAppointment = new AppointmentEntity(location, pet, oldStartsAt, oldStartsAt.plusMinutes(30));

        var newStartsAt = LocalDateTime.of(2025, 6, 1, 11, 0);
        var newEndsAt = newStartsAt.plusMinutes(30);
        RescheduleData rescheduleData = () -> newStartsAt;

        when(appointmentService.retrieveByOwnerAndId(username, 1L)).thenReturn(oldAppointment);
        when(appointmentService.reschedule(username, oldAppointment, newStartsAt, newEndsAt))
                .thenThrow(new tech.petclinix.logic.domain.exception.AppointmentOverlapException(vet.getId(), newStartsAt, newEndsAt));

        //act + assert
        assertThatThrownBy(() -> ownerAppointmentService.reschedule(username, 1L, rescheduleData))
                .isInstanceOf(tech.petclinix.logic.domain.exception.AppointmentOverlapException.class);
        assertThat(oldAppointment.getStatus()).isEqualTo(tech.petclinix.logic.domain.AppointmentStatus.BOOKED);
    }

}
