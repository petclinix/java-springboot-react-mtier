package tech.petclinix.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record LocationResponse(
        Long id,
        String name,
        String zoneId,
        List<OpeningPeriodResponse> weeklyPeriods,
        List<OpeningOverrideResponse> overrides
) {

    public record OpeningPeriodResponse(
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int sortOrder
    ) {
    }

    public record OpeningOverrideResponse(
            LocalDate date,
            LocalTime openTime,
            LocalTime closeTime,
            boolean closed,
            String reason
    ) {
    }
}
