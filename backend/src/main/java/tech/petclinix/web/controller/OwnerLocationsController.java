package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.petclinix.logic.domain.BookableLocation;
import tech.petclinix.logic.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/owner/locations")
@PreAuthorize("hasRole('OWNER')")
public class OwnerLocationsController {

    private final LocationService locationService;

    public OwnerLocationsController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping()
    public ResponseEntity<List<BookableLocation>> retrieveAll() {
        return ResponseEntity.ok(locationService.findAllBookable());
    }
}
