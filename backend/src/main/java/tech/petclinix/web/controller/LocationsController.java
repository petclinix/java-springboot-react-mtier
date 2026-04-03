package tech.petclinix.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.LocationService;
import tech.petclinix.logic.domain.Location;
import java.util.List;

@RestController
@RequestMapping("/locations")
@PreAuthorize("hasRole('VET')")
public class LocationsController {

    private final LocationService locationService;

    public LocationsController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping()
    public ResponseEntity<List<Location>> retrieveAll(Authentication authentication) {
        return ResponseEntity.ok(
                locationService.findAllByVet(new Username(authentication.getName()))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> retrieve(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(
                locationService.findByVetAndId(new Username(authentication.getName()), id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> update(Authentication authentication, @PathVariable Long id,
                                           @Valid @RequestBody Location locationRequest) {
        return ResponseEntity.ok(
                locationService.update(new Username(authentication.getName()), id, locationRequest)
        );
    }

    @PostMapping()
    public ResponseEntity<Location> create(Authentication authentication,
                                           @Valid @RequestBody Location locationRequest) {
        return ResponseEntity.ok(
                locationService.persist(new Username(authentication.getName()), locationRequest)
        );
    }

}
