package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.VetVisitService;
import tech.petclinix.logic.service.VisitService;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.web.dto.VetVisitRequest;
import tech.petclinix.web.dto.VetVisitResponse;

@RestController
@RequestMapping("/vet/visits")
public class VetVisitsController {

    private final VetVisitService vetVisitService;

    public VetVisitsController(VetVisitService vetVisitService) {
        this.vetVisitService = vetVisitService;
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<VetVisitResponse> get(Authentication authentication, @PathVariable Long appointmentId) {
        VisitEntity visit = vetVisitService.retrieveVisit(new Username(authentication.getName()), appointmentId);
        return ResponseEntity.ok(toResponse(visit));
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<VetVisitResponse> put(Authentication authentication, @PathVariable Long appointmentId,
                                                @RequestBody VetVisitRequest request) {
        VisitEntity visit = vetVisitService.persist(new Username(authentication.getName()), appointmentId, request);
        return ResponseEntity.ok(toResponse(visit));
    }

    private VetVisitResponse toResponse(VisitEntity visit) {
        return new VetVisitResponse(visit.getId(), visit.getVetSummary(), visit.getOwnerSummary(), visit.getVaccination());
    }
}
