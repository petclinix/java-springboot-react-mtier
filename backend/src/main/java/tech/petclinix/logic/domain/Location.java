package tech.petclinix.logic.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record Location(
        Long id,
        String name,
        String zoneId,
        @JsonDeserialize(contentAs = OpeningPeriodResponse.class)
        List<? extends PeriodData> weeklyPeriods,
        @JsonDeserialize(contentAs = OpeningOverrideResponse.class)
        List<? extends OverrideData> overrides
) implements LocationData {

    public record OpeningPeriodResponse(
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int sortOrder
    ) implements PeriodData {
    }

    public record OpeningOverrideResponse(
            LocalDate date,
            LocalTime openTime,
            LocalTime closeTime,
            boolean closed,
            String reason
    ) implements OverrideData {
    }
}
