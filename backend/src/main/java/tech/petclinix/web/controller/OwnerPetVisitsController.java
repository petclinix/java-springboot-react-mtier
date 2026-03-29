package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.logic.service.VisitService;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.web.dto.OwnerVisitResponse;

import java.util.List;

@RestController
@RequestMapping("/owner/pets/{petId}/visits")
public class OwnerPetVisitsController {

    private final PetService petService;
    private final VisitService visitService;

    public OwnerPetVisitsController(PetService petService, VisitService visitService) {
        this.petService = petService;
        this.visitService = visitService;
    }

    @GetMapping
    public ResponseEntity<List<OwnerVisitResponse>> list(Authentication authentication, @PathVariable Long petId) {
        PetEntity pet = petService.retrieveByOwnerAndId(authentication.getName(), petId);
        List<OwnerVisitResponse> visits = visitService.findAllByPet(pet).stream()
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
