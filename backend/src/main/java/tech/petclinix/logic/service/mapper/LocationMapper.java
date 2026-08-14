package tech.petclinix.logic.service.mapper;

import tech.petclinix.logic.domain.BookableLocation;
import tech.petclinix.logic.domain.OpeningHours;
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

    private static List<OpeningPeriodResponse> getOpeningPeriodResponses(LocationEntity location) {
        return location.getWeeklyPeriods().stream()
                .map(period -> new OpeningPeriodResponse(period.getDayOfWeek(), period.getStartTime(), period.getEndTime(), period.getSortOrder()))
                .toList();
    }

    private static List<OpeningOverrideResponse> getOpeningOverrideResponses(LocationEntity location) {
        return location.getOverrides().stream()
                .map(exception -> new OpeningOverrideResponse(exception.getDate(), exception.getOpenTime(), exception.getCloseTime(), exception.isClosed(), exception.getReason()))
                .toList();
    }

    public static BookableLocation toBookableLocation(LocationEntity location) {
        return new BookableLocation(
                location.getId(),
                location.getName(),
                location.getVet().getUsername(),
                location.getZoneId(),
                location.getStreet(),
                location.getPostalCode(),
                location.getCity(),
                location.getCountry()
        );
    }

    public static OpeningHours toOpeningHours(LocationEntity location) {
        return new OpeningHours(
                location.getZoneId(),
                location.getWeeklyPeriods().stream()
                        .map(p -> new OpeningHours.WeeklyPeriod(p.getDayOfWeek(), p.getStartTime(), p.getEndTime(), p.getSortOrder()))
                        .toList(),
                location.getOverrides().stream()
                        .map(o -> new OpeningHours.OpeningOverride(o.getDate(), o.getOpenTime(), o.getCloseTime(), o.isClosed(), o.getReason()))
                        .toList()
        );
    }
}
