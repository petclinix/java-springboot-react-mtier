package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.LocationService;
import tech.petclinix.web.controller.mapper.LocationMapper;
import tech.petclinix.web.dto.LocationResponse;

import java.util.List;

import static tech.petclinix.web.controller.mapper.LocationMapper.toLocationResponse;

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
                .map(LocationMapper::toLocationResponse)
                .toList();
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> retrieve(Authentication authentication, @PathVariable Long id) {
        var location = locationService.findByVetAndId(authentication, id);
        return ResponseEntity.ok(toLocationResponse(location));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> update(Authentication authentication, @PathVariable Long id, @RequestBody LocationResponse locationRequest) {
        var location = locationService.update(authentication, id, locationRequest);
        return ResponseEntity.ok(toLocationResponse(location));
    }

    @PostMapping()
    public ResponseEntity<LocationResponse> create(Authentication authentication, @RequestBody LocationResponse locationRequest) {
        var location = locationService.persist(authentication, locationRequest);
        return ResponseEntity.ok(toLocationResponse(location));
    }

}
