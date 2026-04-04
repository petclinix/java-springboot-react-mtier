package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Vet;
import tech.petclinix.logic.service.VetService;

import java.util.List;

@RestController
@RequestMapping("/vets")
@PreAuthorize("hasRole('OWNER')")
public class VetsController {

    private final VetService vetService;

    public VetsController(VetService vetService) {
        this.vetService = vetService;
    }

    @GetMapping()
    public ResponseEntity<List<Vet>> retrieveAll() {
        return ResponseEntity.ok(
                vetService.findAll()
        );
    }

}
