package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.PetVisitService;
import tech.petclinix.logic.domain.OwnerVisit;

import java.util.List;

@RestController
@RequestMapping("/owner/pets/{petId}/visits")
public class OwnerPetVisitsController {

    private final PetVisitService petVisitService;

    public OwnerPetVisitsController(PetVisitService petVisitService) {
        this.petVisitService = petVisitService;
    }

    @GetMapping
    public ResponseEntity<List<OwnerVisit>> list(Authentication authentication, @PathVariable Long petId) {
        return ResponseEntity.ok(
                petVisitService.findAllVisitsByOwnerAndPet(new Username(authentication.getName()), petId)
        );
    }

}
