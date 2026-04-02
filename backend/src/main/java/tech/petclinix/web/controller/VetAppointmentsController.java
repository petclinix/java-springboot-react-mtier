package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.AppointmentService;
import tech.petclinix.logic.domain.VetAppointment;

import java.util.List;

@RestController
@RequestMapping("/vet/appointments")
public class VetAppointmentsController {

    private final AppointmentService appointmentService;

    public VetAppointmentsController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<VetAppointment>> list(Authentication authentication) {
        return ResponseEntity.ok(
                appointmentService.findAllByVet(new Username(authentication.getName()))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long id) {
        appointmentService.cancelByVet(new Username(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }

}
