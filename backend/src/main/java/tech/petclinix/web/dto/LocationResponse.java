package tech.petclinix.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record LocationResponse(
        Long id,
        String name,
        String zoneId,
        List<OpeningPeriodResponse> weeklyPeriods,
        List<OpeningExceptionResponse> exceptions
) {

    public record OpeningPeriodResponse(
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int sortOrder
    ) {
    }

    public record OpeningExceptionResponse(
            LocalDate date,
            boolean closed,
            String note
    ) {
    }
}
