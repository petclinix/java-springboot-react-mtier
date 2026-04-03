package tech.petclinix.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Pet;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.web.dto.PetRequest;

import java.util.List;

@RestController
@RequestMapping("/pets")
@PreAuthorize("hasRole('OWNER')")
public class PetsController {

    private final PetService petService;

    public PetsController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping()
    public ResponseEntity<List<Pet>> retrieveAll(Authentication authentication) {
        return ResponseEntity.ok(
                petService.findAllByOwner(new Username(authentication.getName()))
        );
    }

    @PostMapping()
    public ResponseEntity<Pet> create(Authentication authentication,
                                      @Valid @RequestBody PetRequest petRequest) {
        return ResponseEntity.ok(
                petService.persist(new Username(authentication.getName()), petRequest.name())
        );
    }
}
