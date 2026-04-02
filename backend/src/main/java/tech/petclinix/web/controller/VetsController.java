package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.VetService;

@RestController
@RequestMapping("/vets")
public class VetsController {

    private final VetService vetService;

    public VetsController(VetService vetService) {
        this.vetService = vetService;
    }

    @GetMapping()
    public ResponseEntity<?> retrieveAll() {
        return ResponseEntity.ok(
                vetService.findAll()
        );
    }

}
