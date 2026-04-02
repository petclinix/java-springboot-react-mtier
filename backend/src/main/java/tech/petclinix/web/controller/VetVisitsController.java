package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.VetVisitService;
import tech.petclinix.web.dto.VetVisitRequest;
import tech.petclinix.logic.domain.VetVisit;

@RestController
@RequestMapping("/vet/visits")
public class VetVisitsController {

    private final VetVisitService vetVisitService;

    public VetVisitsController(VetVisitService vetVisitService) {
        this.vetVisitService = vetVisitService;
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<VetVisit> get(Authentication authentication, @PathVariable Long appointmentId) {
        return ResponseEntity.ok(
                vetVisitService.retrieveByVetAndId(new Username(authentication.getName()), appointmentId)
        );
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<VetVisit> put(Authentication authentication, @PathVariable Long appointmentId,
                                        @RequestBody VetVisitRequest request) {
        return ResponseEntity.ok(
                vetVisitService.persist(new Username(authentication.getName()), appointmentId, request)
        );
    }

}
