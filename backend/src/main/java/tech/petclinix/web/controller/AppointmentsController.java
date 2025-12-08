package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.AppointmentService;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.web.dto.AppointmentRequest;
import tech.petclinix.web.dto.AppointmentResponse;
import tech.petclinix.web.dto.PetResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/appointments")
public class AppointmentsController {

    private final AppointmentService appointmentService;

    public AppointmentsController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping()
    public ResponseEntity<?> create(Authentication authentication, @RequestBody AppointmentRequest appointmentRequest) {
        var appointment = appointmentService.persist(appointmentRequest.vetId(), appointmentRequest.petId(), appointmentRequest.startsAt(), authentication);
        return ResponseEntity.ok(new AppointmentResponse(appointment.getId(), appointment.getVet().getId(), appointment.getPet().getId(), appointment.getStartAt()));
    }
}
