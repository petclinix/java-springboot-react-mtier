package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.LocationService;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.web.dto.LocationResponse;
import tech.petclinix.web.dto.LocationResponse.OpeningPeriodResponse;
import tech.petclinix.web.dto.LocationResponse.OpeningOverrideResponse;

import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationsController {

    private final LocationService locationService;

    public LocationsController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping()
    public ResponseEntity<List<LocationResponse>> retrieveAll(Authentication authentication) {
        var locations = locationService.findAllByVet(authentication).stream()
                .map(location -> toLocationResponse(location))
                .toList();
        return ResponseEntity.ok(locations);
    }


    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> retrieve(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(toLocationResponse(locationService.findByVetAndId(authentication, id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> update(Authentication authentication, @PathVariable Long id, @RequestBody LocationResponse locationRequest) {
        return ResponseEntity.ok(toLocationResponse(locationService.update(authentication, id, locationRequest)));
    }

    @PostMapping()
    public ResponseEntity<LocationResponse> create(Authentication authentication, @RequestBody LocationResponse locationRequest) {
        var location = locationService.persist(authentication, locationRequest);
        return ResponseEntity.ok(toLocationResponse(location));
    }

    private static LocationResponse toLocationResponse(LocationEntity location) {
        return new LocationResponse(location.getId(), location.getName(), location.getZoneId(),
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

}
