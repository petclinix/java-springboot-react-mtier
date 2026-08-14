package tech.petclinix.logic.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Domain record for a clinic location. Also serves as the request and response body for
 * the locations API — no separate DTO exists because the structures are identical.
 * See {@link LocationData} for the rationale and the divergence strategy.
 *
 * <p>{@code weeklyPeriods}/{@code overrides} are typed concretely here (not as the
 * {@code List<? extends PeriodData>}/{@code List<? extends OverrideData>} wildcard types
 * {@link LocationData} declares) — a record's accessor may covariantly narrow an interface
 * method's return type, so this still satisfies {@code implements LocationData}. Concrete
 * types let Jackson deserialize without a {@code @JsonDeserialize} hint and let OpenAPI
 * generate a precise array-item schema instead of an unresolvable {@code unknown}.
 */
public record Location(
        Long id,
        String name,
        String zoneId,
        String street,
        String postalCode,
        String city,
        String country,
        List<OpeningPeriodResponse> weeklyPeriods,
        List<OpeningOverrideResponse> overrides
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
