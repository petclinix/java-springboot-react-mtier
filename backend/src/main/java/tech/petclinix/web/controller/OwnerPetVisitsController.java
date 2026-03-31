package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.PetVisitService;
import tech.petclinix.web.dto.OwnerVisitResponse;

import java.util.List;

@RestController
@RequestMapping("/owner/pets/{petId}/visits")
public class OwnerPetVisitsController {

    private final PetVisitService petVisitService;

    public OwnerPetVisitsController(PetVisitService petVisitService) {
        this.petVisitService = petVisitService;
    }

    @GetMapping
    public ResponseEntity<List<OwnerVisitResponse>> list(Authentication authentication, @PathVariable Long petId) {
        List<OwnerVisitResponse> visits = petVisitService.findAllVisitsByOwnerAndPet(new Username(authentication.getName()), petId).stream()
                .map(v -> new OwnerVisitResponse(
                        v.getId(),
                        v.getOwnerSummary(),
                        v.getVaccination(),
                        v.getAppointment().getVet().getUsername(),
                        v.getAppointment().getStartAt()
                ))
                .toList();
        return ResponseEntity.ok(visits);
    }
}
