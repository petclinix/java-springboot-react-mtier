package tech.petclinix.web.controller.mapper;

import tech.petclinix.logic.domain.LocationData;
import tech.petclinix.logic.domain.LocationData.PeriodData;
import tech.petclinix.logic.domain.LocationData.OverrideData;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.web.dto.LocationResponse;
import tech.petclinix.web.dto.LocationResponse.OpeningPeriodResponse;
import tech.petclinix.web.dto.LocationResponse.OpeningOverrideResponse;

import java.util.List;

public class LocationMapper {
    public static LocationResponse toLocationResponse(LocationEntity location) {
        return new LocationResponse(location.getId(), location.getName(), location.getZoneId(),
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
