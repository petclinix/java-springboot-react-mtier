package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.OwnerAppointmentService;
import tech.petclinix.web.controller.mapper.DtoMapper;
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
                .map(DtoMapper::toAppointmentResponse)
                .toList();
        return ResponseEntity.ok(appointments);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(Authentication authentication, @RequestBody AppointmentRequest appointmentRequest) {
        var appointment = appointmentService.persist(new Username(authentication.getName()), appointmentRequest);
        return ResponseEntity.ok(DtoMapper.toAppointmentResponse(appointment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long id) {
        appointmentService.cancelByOwner(new Username(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }

}
