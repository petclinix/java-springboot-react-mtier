package tech.petclinix.web.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.petclinix.logic.domain.AvailableSlot;
import tech.petclinix.logic.domain.BookableLocation;
import tech.petclinix.logic.service.AvailabilityService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/owner/locations")
@PreAuthorize("hasRole('OWNER')")
public class OwnerLocationsController {

    private final AvailabilityService availabilityService;

    public OwnerLocationsController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping()
    public ResponseEntity<List<BookableLocation>> retrieveAll() {
        return ResponseEntity.ok(availabilityService.findAllBookable());
    }

    @GetMapping("/{id}/available-slots")
    public ResponseEntity<List<AvailableSlot>> retrieveAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.findAvailableSlots(id, date));
    }
}
