package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.VetService;
import tech.petclinix.web.controller.mapper.DtoMapper;

@RestController
@RequestMapping("/vets")
public class VetsController {

    private final VetService vetService;

    public VetsController(VetService vetService) {
        this.vetService = vetService;
    }

    @GetMapping()
    public ResponseEntity<?> retrieveAll() {
        var pets = vetService.findAll().stream()
                .map(DtoMapper::toVetResponse)
                .toList();
        return ResponseEntity.ok(pets);
    }

}
