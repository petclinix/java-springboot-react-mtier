package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.web.dto.PetRequest;
import tech.petclinix.web.dto.PetResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/pets")
public class PetsController {

    private final PetService petService;

    public PetsController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping()
    public ResponseEntity<?> retrieveAll(Authentication authentication) {
        var pets = petService.findAllByOwner(new Username(authentication.getName())).stream()
                .map(pet -> new PetResponse(pet.id(), pet.name(), "", "", LocalDate.now()))
                .toList();
        return ResponseEntity.ok(pets);
    }

    @PostMapping()
    public ResponseEntity<?> create(Authentication authentication, @RequestBody PetRequest petRequest) {
        var pet = petService.persist(new Username(authentication.getName()), petRequest.name());
        return ResponseEntity.ok(new PetResponse(pet.id(), pet.name(), "", "", LocalDate.now()));
    }
}
