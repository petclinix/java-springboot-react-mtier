package tech.petclinix.logic.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Contract for location write operations accepted by {@link tech.petclinix.logic.service.LocationService}.
 *
 * <p>The service accepts this interface, not a concrete DTO, so that the web layer can pass
 * its own type without the logic layer importing from {@code web}. Currently {@link Location}
 * implements this interface directly — it serves as both the domain record and the
 * request/response body. No separate DTO exists because the structures are identical.
 *
 * <p>If the API shape and the domain shape ever diverge (e.g. the API needs a field the
 * domain does not, or vice versa), introduce a dedicated {@code LocationRequest} record in
 * {@code web/dto} that also implements this interface. The service signature does not change.
 */
public interface LocationData {
    String name();

    String zoneId();

    String street();

    String postalCode();

    String city();

    String country();

    List<? extends PeriodData> weeklyPeriods();

    List<? extends OverrideData> overrides();

    interface PeriodData {
        int dayOfWeek();

        LocalTime startTime();

        LocalTime endTime();

        int sortOrder();
    }

    interface OverrideData {
        LocalDate date();

        LocalTime openTime();

        LocalTime closeTime();

        boolean closed();

        String reason();
    }
}
