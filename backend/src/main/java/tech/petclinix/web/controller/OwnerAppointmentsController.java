package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.OwnerAppointmentService;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.web.dto.AppointmentRequest;
import tech.petclinix.web.dto.AppointmentResponse;

import java.util.List;

@RestController
@RequestMapping("/owner/appointments")
public class OwnerAppointmentsController {

    private final OwnerAppointmentService appointmentService;

    public OwnerAppointmentsController(OwnerAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> list(Authentication authentication) {
        List<AppointmentResponse> appointments = appointmentService.findAllByOwner(new Username(authentication.getName())).stream()
                .map(a -> toAppointmentResponse(a))
                .toList();
        return ResponseEntity.ok(appointments);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(Authentication authentication, @RequestBody AppointmentRequest appointmentRequest) {
        var appointment = appointmentService.persist(new Username(authentication.getName()), appointmentRequest);
        return ResponseEntity.ok(toAppointmentResponse(appointment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long id) {
        appointmentService.cancelByOwner(new Username(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }

    private static AppointmentResponse toAppointmentResponse(AppointmentEntity a) {
        return new AppointmentResponse(a.getId(), a.getVet().getId(), a.getPet().getId(), a.getStartAt());
    }

}
