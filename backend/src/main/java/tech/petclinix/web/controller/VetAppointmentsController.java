package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.AppointmentService;
import tech.petclinix.logic.domain.VetAppointment;
import tech.petclinix.logic.service.VetAppointmentService;

import java.util.List;

@RestController
@RequestMapping("/vet/appointments")
@PreAuthorize("hasRole('VET')")
public class VetAppointmentsController {

    private final VetAppointmentService vetAppointmentService;

    public VetAppointmentsController(VetAppointmentService vetAppointmentService) {
        this.vetAppointmentService = vetAppointmentService;
    }

    @GetMapping
    public ResponseEntity<List<VetAppointment>> list(Authentication authentication) {
        return ResponseEntity.ok(
                vetAppointmentService.findAllByVet(new Username(authentication.getName()))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long id) {
        vetAppointmentService.cancelByVet(new Username(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }

}
