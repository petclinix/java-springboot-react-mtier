package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.LocationService;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.web.dto.LocationResponse;
import tech.petclinix.web.dto.LocationResponse.OpeningPeriodResponse;
import tech.petclinix.web.dto.LocationResponse.OpeningExceptionResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationsController {

    private final LocationService locationService;

    public LocationsController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping()
    public ResponseEntity<?> retrieveAll(Authentication authentication) {
        var locations = locationService.findAllByVet(authentication).stream()
                .map(location -> toLocationResponse(location))
                .toList();
        return ResponseEntity.ok(locations);
    }

    private static LocationResponse toLocationResponse(LocationEntity location) {
        return new LocationResponse(location.getId(), location.getName(), location.getZoneId(),
                getOpeningPeriodResponses(location),
                getOpeningExceptionResponses(location)
        );
    }

    private static List<OpeningPeriodResponse> getOpeningPeriodResponses(LocationEntity location) {
        return location.getWeeklyPeriods().stream()
                .map(period -> new OpeningPeriodResponse(period.getDayOfWeek(), period.getStartTime(), period.getEndTime(), period.getSortOrder()))
                .toList();
    }

    private static List<OpeningExceptionResponse> getOpeningExceptionResponses(LocationEntity location) {
        return location.getExceptions().stream()
                .map(exception -> new OpeningExceptionResponse(exception.getDate(), exception.isClosed(), exception.getNote()))
                .toList();
    }

}
