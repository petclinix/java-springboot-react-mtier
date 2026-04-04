package tech.petclinix.logic.service.mapper;

import tech.petclinix.logic.domain.LocationData.PeriodData;
import tech.petclinix.logic.domain.LocationData.OverrideData;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.logic.domain.Location;
import tech.petclinix.logic.domain.Location.OpeningPeriodResponse;
import tech.petclinix.logic.domain.Location.OpeningOverrideResponse;

import java.util.List;

public class LocationMapper {
    public static Location toLocation(LocationEntity location) {
        return new Location(
                location.getId(),
                location.getName(),
                location.getZoneId(),
                location.getStreet(),
                location.getPostalCode(),
                location.getCity(),
                location.getCountry(),
                getOpeningPeriodResponses(location),
                getOpeningOverrideResponses(location)
        );
    }

    private static List<? extends PeriodData> getOpeningPeriodResponses(LocationEntity location) {
        return location.getWeeklyPeriods().stream()
                .map(period -> new OpeningPeriodResponse(period.getDayOfWeek(), period.getStartTime(), period.getEndTime(), period.getSortOrder()))
                .toList();
    }

    private static List<? extends OverrideData> getOpeningOverrideResponses(LocationEntity location) {
        return location.getOverrides().stream()
                .map(exception -> new OpeningOverrideResponse(exception.getDate(), exception.getOpenTime(), exception.getCloseTime(), exception.isClosed(), exception.getReason()))
                .toList();
    }
}
