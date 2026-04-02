package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.OwnerAppointmentService;
import tech.petclinix.web.controller.mapper.DtoMapper;
import tech.petclinix.web.dto.AppointmentRequest;
import tech.petclinix.logic.domain.Appointment;

import java.util.List;

@RestController
@RequestMapping("/owner/appointments")
public class OwnerAppointmentsController {

    private final OwnerAppointmentService appointmentService;

    public OwnerAppointmentsController(OwnerAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> list(Authentication authentication) {
        return ResponseEntity.ok(
                appointmentService.findAllByOwner(new Username(authentication.getName()))
        );
    }

    @PostMapping
    public ResponseEntity<Appointment> create(Authentication authentication, @RequestBody AppointmentRequest appointmentRequest) {
        return ResponseEntity.ok(
                appointmentService.persist(new Username(authentication.getName()), appointmentRequest)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long id) {
        appointmentService.cancelByOwner(new Username(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }

}
