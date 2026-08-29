package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.AvailableSlot;
import tech.petclinix.logic.domain.BookableLocation;
import tech.petclinix.logic.domain.OpeningHours;
import tech.petclinix.logic.service.mapper.LocationMapper;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrating service (CLAUDE.md rule 8): coordinates {@link LocationService} and
 * {@link AppointmentService} to derive the owner-facing view of a location — bookable locations,
 * and the truly open slots on a given day. Owns no repository of its own.
 */
@Service
public class AvailabilityService {

    private final LocationService locationService;
    private final AppointmentService appointmentService;
    private final Clock clock;

    public AvailabilityService(LocationService locationService, AppointmentService appointmentService, Clock clock) {
        this.locationService = locationService;
        this.appointmentService = appointmentService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BookableLocation> findAllBookable() {
        return locationService.findAllBookable();
    }

    /**
     * Available slots for the given location on the given date: opening hours (weekly pattern,
     * or the day's override if one exists) minus active (BOOKED/CONFIRMED) appointments minus
     * already-past slots, at {@link AppointmentService#DEFAULT_DURATION_MINUTES}-minute
     * increments, sorted chronologically.
     */
    @Transactional(readOnly = true)
    public List<AvailableSlot> findAvailableSlots(Long locationId, LocalDate date) {
        LocationEntity location = locationService.retrieveById(locationId);
        OpeningHours openingHours = LocationMapper.toOpeningHours(location);
        List<TimeWindow> windows = resolveWindows(openingHours, date);
        if (windows.isEmpty()) {
            return List.of();
        }

        VetEntity vet = location.getVet();
        List<AppointmentEntity> activeAppointments = appointmentService.findActiveByVetOnDate(vet, date);
        LocalDateTime now = LocalDateTime.now(clock);

        List<AvailableSlot> slots = new ArrayList<>();
        for (TimeWindow window : windows) {
            LocalTime slotStart = window.start();
            while (true) {
                LocalTime slotEnd = slotStart.plusMinutes(AppointmentService.DEFAULT_DURATION_MINUTES);
                if (slotEnd.isBefore(slotStart) || slotEnd.isAfter(window.end())) {
                    // Either the window ran out of room for a full slot, or adding the duration
                    // wrapped past midnight (LocalTime wraps rather than overflowing) — both mean
                    // this candidate doesn't fully fit inside the window.
                    break;
                }
                LocalDateTime candidateStart = LocalDateTime.of(date, slotStart);
                LocalDateTime candidateEnd = LocalDateTime.of(date, slotEnd);
                if (candidateStart.isAfter(now) && !overlapsAny(candidateStart, candidateEnd, activeAppointments)) {
                    slots.add(new AvailableSlot(candidateStart, candidateEnd));
                }
                slotStart = slotEnd;
            }
        }

        slots.sort(Comparator.comparing(AvailableSlot::startsAt));
        return slots;
    }

    private List<TimeWindow> resolveWindows(OpeningHours openingHours, LocalDate date) {
        OpeningHours.OpeningOverride override = openingHours.overrides().stream()
                .filter(o -> o.date().equals(date))
                .findFirst()
                .orElse(null);

        if (override != null) {
            if (override.closed() || override.openTime() == null || override.closeTime() == null) {
                return List.of();
            }
            return List.of(new TimeWindow(override.openTime(), override.closeTime()));
        }

        int dowVal = date.getDayOfWeek().getValue();
        return openingHours.weeklyPeriods().stream()
                .filter(p -> p.dayOfWeek() == dowVal)
                .sorted(Comparator.comparingInt(OpeningHours.WeeklyPeriod::sortOrder))
                .map(p -> new TimeWindow(p.startTime(), p.endTime()))
                .toList();
    }

    private boolean overlapsAny(LocalDateTime candidateStart, LocalDateTime candidateEnd, List<AppointmentEntity> appointments) {
        return appointments.stream().anyMatch(a ->
                candidateStart.isBefore(a.getEndsAt()) && candidateEnd.isAfter(a.getStartAt()));
    }

    private record TimeWindow(LocalTime start, LocalTime end) {
    }
}
