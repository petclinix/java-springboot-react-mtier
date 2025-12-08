package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.AppointmentService;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.logic.service.VetService;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.web.dto.AppointmentRequest;
import tech.petclinix.web.dto.AppointmentResponse;

@RestController
@RequestMapping("/appointments")
public class AppointmentsController {

    private final AppointmentService appointmentService;
    private final PetService petService;
    private final VetService vetService;

    public AppointmentsController(AppointmentService appointmentService, PetService petService, VetService vetService) {
        this.appointmentService = appointmentService;
        this.petService = petService;
        this.vetService = vetService;
    }

    @PostMapping()
    public ResponseEntity<?> create(Authentication authentication, @RequestBody AppointmentRequest appointmentRequest) {
        PetEntity pet = petService.retrieveByOwnerAndId(authentication.getName(), appointmentRequest.petId());
        VetEntity vet = vetService.retrieveById(appointmentRequest.vetId());

        var appointment = appointmentService.persist(pet, vet, appointmentRequest.startsAt());
        return ResponseEntity.ok(new AppointmentResponse(appointment.getId(), appointment.getVet().getId(), appointment.getPet().getId(), appointment.getStartAt()));
    }
}
