package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.logic.service.UserType;
import tech.petclinix.web.dto.PetRequest;
import tech.petclinix.web.dto.PetResponse;
import tech.petclinix.web.dto.UserResponse;

@RestController
@RequestMapping("/pets")
public class PetsController {

    private final PetService petService;

    public PetsController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/")
    public ResponseEntity<?> retrieveAll() {
        var pets = petService.findAll().stream()
                .map(pet -> new PetResponse(pet.id(), pet.name()))
                .toList();
        return ResponseEntity.ok(pets);
    }

    @PostMapping("/")
    public ResponseEntity<?> create(@RequestBody PetRequest petRequest) {
        var pet = petService.persist(petRequest.name());
        return ResponseEntity.ok(new PetResponse(pet.id(), pet.name()));
    }
}
