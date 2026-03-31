package tech.petclinix.web.controller.mapper;

import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.web.dto.LocationResponse;

import java.util.List;

public class LocationMapper {
    public static LocationResponse toLocationResponse(LocationEntity location) {
        return new LocationResponse(location.getId(), location.getName(), location.getZoneId(),
                getOpeningPeriodResponses(location),
                getOpeningOverrideResponses(location)
        );
    }

    private static List<LocationResponse.OpeningPeriodResponse> getOpeningPeriodResponses(LocationEntity location) {
        return location.getWeeklyPeriods().stream()
                .map(period -> new LocationResponse.OpeningPeriodResponse(period.getDayOfWeek(), period.getStartTime(), period.getEndTime(), period.getSortOrder()))
                .toList();
    }

    private static List<LocationResponse.OpeningOverrideResponse> getOpeningOverrideResponses(LocationEntity location) {
        return location.getOverrides().stream()
                .map(exception -> new LocationResponse.OpeningOverrideResponse(exception.getDate(), exception.getOpenTime(), exception.getCloseTime(), exception.isClosed(), exception.getReason()))
                .toList();
    }
}
