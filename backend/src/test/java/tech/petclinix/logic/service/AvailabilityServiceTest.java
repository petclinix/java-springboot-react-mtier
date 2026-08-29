package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.AppointmentType;
import tech.petclinix.logic.domain.AvailableSlot;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.OpeningOverrideEntity;
import tech.petclinix.persistence.entity.OpeningPeriodEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AvailabilityService}.
 *
 * {@link LocationService} and {@link AppointmentService} are mocked — no database.
 * {@link tech.petclinix.logic.service.mapper.LocationMapper} is a pure static utility and is
 * exercised through the real {@link LocationEntity} fixtures below rather than mocked.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7); // a Monday
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 7, 8, 0);

    @Mock
    private LocationService locationService;

    @Mock
    private AppointmentService appointmentService;

    private Clock clock;
    private AvailabilityService availabilityService;
    private VetEntity vet;
    private LocationEntity location;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        availabilityService = new AvailabilityService(locationService, appointmentService, clock);
        vet = new VetEntity("vet-jack", "hash");
        location = new LocationEntity(vet, "Clinic North", "UTC");
    }

    /** Generates fully-fitting 30-minute slots across a single weekly opening period. */
    @Test
    void findAvailableSlotsGeneratesSlotsFromWeeklyPeriod() {
        //arrange
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0));
        when(locationService.retrieveById(1L)).thenReturn(location);
        when(appointmentService.findActiveByVetOnDate(vet, MONDAY)).thenReturn(List.of());

        //act
        var result = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.CHECKUP);

        //assert
        assertThat(result).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 30)),
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 30), LocalDateTime.of(2026, 9, 7, 10, 0))
        );
    }

    /**
     * The generated slot list is duration-aware: a short appointment type (VACCINATION, 15 min)
     * produces more, tighter slots than a long appointment type (SURGERY, 60 min) for the exact
     * same opening window and no existing appointments.
     */
    @Test
    void findAvailableSlotsProducesDifferentSlotListsForDifferentAppointmentTypeDurations() {
        //arrange
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0));
        when(locationService.retrieveById(1L)).thenReturn(location);
        when(appointmentService.findActiveByVetOnDate(vet, MONDAY)).thenReturn(List.of());

        //act
        var vaccinationSlots = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.VACCINATION);
        var surgerySlots = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.SURGERY);

        //assert — 60 minutes / 15-minute slots = 4 slots; 60 minutes / 60-minute slots = 1 slot
        assertThat(vaccinationSlots).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 15)),
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 15), LocalDateTime.of(2026, 9, 7, 9, 30)),
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 30), LocalDateTime.of(2026, 9, 7, 9, 45)),
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 45), LocalDateTime.of(2026, 9, 7, 10, 0))
        );
        assertThat(surgerySlots).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 10, 0))
        );
    }

    /** Generates independent slot sequences for each shift when a day has multiple weekly periods. */
    @Test
    void findAvailableSlotsGeneratesSlotsForEachShiftIndependently() {
        //arrange
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0));
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(14, 0), LocalTime.of(15, 0), 1));
        when(locationService.retrieveById(1L)).thenReturn(location);
        when(appointmentService.findActiveByVetOnDate(vet, MONDAY)).thenReturn(List.of());

        //act
        var result = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.CHECKUP);

        //assert
        assertThat(result).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 30)),
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 30), LocalDateTime.of(2026, 9, 7, 10, 0)),
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 14, 0), LocalDateTime.of(2026, 9, 7, 14, 30)),
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 14, 30), LocalDateTime.of(2026, 9, 7, 15, 0))
        );
    }

    /** Does not generate a trailing partial slot when the window doesn't evenly divide by the slot duration. */
    @Test
    void findAvailableSlotsExcludesPartialTrailingSlot() {
        //arrange
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(9, 45), 0));
        when(locationService.retrieveById(1L)).thenReturn(location);
        when(appointmentService.findActiveByVetOnDate(vet, MONDAY)).thenReturn(List.of());

        //act
        var result = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.CHECKUP);

        //assert
        assertThat(result).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 30))
        );
    }

    /** Uses the date-specific override's custom hours in place of the weekly pattern when one exists. */
    @Test
    void findAvailableSlotsUsesOverrideHoursInsteadOfWeeklyPattern() {
        //arrange
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 0));
        location.addOverride(new OpeningOverrideEntity(location, MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0), false, "Late start"));
        when(locationService.retrieveById(1L)).thenReturn(location);
        when(appointmentService.findActiveByVetOnDate(vet, MONDAY)).thenReturn(List.of());

        //act
        var result = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.CHECKUP);

        //assert
        assertThat(result).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 11, 0), LocalDateTime.of(2026, 9, 7, 11, 30)),
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 11, 30), LocalDateTime.of(2026, 9, 7, 12, 0))
        );
    }

    /** Returns an empty list when the override marks the date fully closed, ignoring the weekly pattern. */
    @Test
    void findAvailableSlotsReturnsEmptyListWhenOverrideIsClosed() {
        //arrange
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 0));
        location.addOverride(new OpeningOverrideEntity(location, MONDAY, null, null, true, "Holiday"));
        when(locationService.retrieveById(1L)).thenReturn(location);

        //act
        var result = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.CHECKUP);

        //assert
        assertThat(result).isEmpty();
    }

    /** Excludes a candidate slot that overlaps an existing BOOKED appointment. */
    @Test
    void findAvailableSlotsExcludesSlotOverlappingBookedAppointment() {
        //arrange
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0));
        var owner = new OwnerEntity("grace", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var booked = new AppointmentEntity(location, pet,
                LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 30), AppointmentType.CHECKUP);
        when(locationService.retrieveById(1L)).thenReturn(location);
        when(appointmentService.findActiveByVetOnDate(vet, MONDAY)).thenReturn(List.of(booked));

        //act
        var result = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.CHECKUP);

        //assert
        assertThat(result).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 30), LocalDateTime.of(2026, 9, 7, 10, 0))
        );
    }

    /** Excludes a candidate slot that overlaps an existing CONFIRMED appointment. */
    @Test
    void findAvailableSlotsExcludesSlotOverlappingConfirmedAppointment() {
        //arrange
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0));
        var owner = new OwnerEntity("grace", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var confirmed = new AppointmentEntity(location, pet,
                LocalDateTime.of(2026, 9, 7, 9, 30), LocalDateTime.of(2026, 9, 7, 10, 0), AppointmentType.CHECKUP);
        confirmed.confirm();
        when(locationService.retrieveById(1L)).thenReturn(location);
        when(appointmentService.findActiveByVetOnDate(vet, MONDAY)).thenReturn(List.of(confirmed));

        //act
        var result = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.CHECKUP);

        //assert
        assertThat(result).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 30))
        );
    }

    /** Excludes slots whose start time is not strictly after "now" for today's date, using the injected Clock. */
    @Test
    void findAvailableSlotsExcludesPastSlotsForToday() {
        //arrange — clock is fixed at 2026-09-07T08:00; window covers a slot before and after "now"
        location.addWeeklyPeriod(new OpeningPeriodEntity(location, MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(7, 0), LocalTime.of(9, 0), 0));
        when(locationService.retrieveById(1L)).thenReturn(location);
        when(appointmentService.findActiveByVetOnDate(vet, MONDAY)).thenReturn(List.of());

        //act
        var result = availabilityService.findAvailableSlots(1L, MONDAY, AppointmentType.CHECKUP);

        //assert — 07:00, 07:30 and 08:00 slots are not strictly after 08:00 "now"; only 08:30 remains
        assertThat(result).containsExactly(
                new AvailableSlot(LocalDateTime.of(2026, 9, 7, 8, 30), LocalDateTime.of(2026, 9, 7, 9, 0))
        );
    }

    /** Throws NotFoundException when the location does not exist. */
    @Test
    void findAvailableSlotsThrowsNotFoundWhenLocationDoesNotExist() {
        //arrange
        when(locationService.retrieveById(99L)).thenThrow(new NotFoundException("Location not found: 99"));

        //act + assert
        assertThatThrownBy(() -> availabilityService.findAvailableSlots(99L, MONDAY, AppointmentType.CHECKUP))
                .isInstanceOf(NotFoundException.class);
    }

    /** Delegates findAllBookable straight through to LocationService. */
    @Test
    void findAllBookableDelegatesToLocationService() {
        //arrange
        when(locationService.findAllBookable()).thenReturn(List.of());

        //act
        var result = availabilityService.findAllBookable();

        //assert
        assertThat(result).isEmpty();
    }
}
