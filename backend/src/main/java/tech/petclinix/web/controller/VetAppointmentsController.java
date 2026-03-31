package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.AppointmentService;
import tech.petclinix.web.dto.VetAppointmentResponse;

import java.util.List;

@RestController
@RequestMapping("/vet/appointments")
public class VetAppointmentsController {

    private final AppointmentService appointmentService;

    public VetAppointmentsController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<VetAppointmentResponse>> list(Authentication authentication) {
        List<VetAppointmentResponse> appointments = appointmentService.findAllByVet(new Username(authentication.getName()))
                .stream()
                .map(a -> new VetAppointmentResponse(
                        a.getId(),
                        a.getPet().getId(),
                        a.getPet().getName(),
                        a.getPet().getOwner().getUsername(),
                        a.getStartAt()))
                .toList();
        return ResponseEntity.ok(appointments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long id) {
        appointmentService.cancelByVet(new Username(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }
}
