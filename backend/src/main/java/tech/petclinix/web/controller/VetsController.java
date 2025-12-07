package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.logic.service.VetService;
import tech.petclinix.web.dto.PetRequest;
import tech.petclinix.web.dto.PetResponse;
import tech.petclinix.web.dto.VetResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/vets")
public class VetsController {

    private final VetService vetService;

    public VetsController(VetService vetService) {
        this.vetService = vetService;
    }

    @GetMapping()
    public ResponseEntity<?> retrieveAll(Authentication authentication) {
        var pets = vetService.findAll().stream()
                .map(vet -> new VetResponse(vet.getId(), vet.getUsername()))
                .toList();
        return ResponseEntity.ok(pets);
    }

}
