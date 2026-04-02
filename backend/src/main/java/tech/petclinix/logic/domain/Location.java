package tech.petclinix.logic.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Domain record for a clinic location. Also serves as the request and response body for
 * the locations API — no separate DTO exists because the structures are identical.
 * See {@link LocationData} for the rationale and the divergence strategy.
 *
 * <p>Note: {@code @JsonDeserialize} hints are required because Jackson cannot infer the
 * concrete type for {@code List<? extends PeriodData>} and {@code List<? extends OverrideData>}
 * without them.
 */
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
