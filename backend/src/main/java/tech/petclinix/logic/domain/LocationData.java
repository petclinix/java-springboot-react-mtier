package tech.petclinix.logic.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// logic/domain/
public interface LocationData {
    String name();

    String zoneId();

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
