package tech.petclinix.logic.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public record OpeningHours(
        String zoneId,
        List<WeeklyPeriod> weeklyPeriods,
        List<OpeningOverride> overrides
) {
    public boolean isOpenAt(Instant instantUtc) {
        ZoneId zone = ZoneId.of(this.zoneId);
        ZonedDateTime zdt = instantUtc.atZone(zone);
        LocalDate localDate = zdt.toLocalDate();
        DayOfWeek dow = zdt.getDayOfWeek();
        LocalTime time = zdt.toLocalTime();

        OpeningOverride override = overrides.stream()
                .filter(o -> o.date().equals(localDate))
                .findFirst()
                .orElse(null);

        if (override != null) {
            if (override.closed()) return false;
            if (override.openTime() == null || override.closeTime() == null) return false;
            return isTimeInPeriod(time, override.openTime(), override.closeTime());
        }

        int dowVal = dow.getValue();
        return weeklyPeriods.stream()
                .filter(p -> p.dayOfWeek() == dowVal)
                .anyMatch(p -> isTimeInPeriod(time, p.startTime(), p.endTime()));
    }

    private static boolean isTimeInPeriod(LocalTime t, LocalTime start, LocalTime end) {
        return (!t.isBefore(start)) && t.isBefore(end);
    }

    public record WeeklyPeriod(int dayOfWeek, LocalTime startTime, LocalTime endTime, int sortOrder) {}

    public record OpeningOverride(LocalDate date, LocalTime openTime, LocalTime closeTime, boolean closed, String reason) {}
}
