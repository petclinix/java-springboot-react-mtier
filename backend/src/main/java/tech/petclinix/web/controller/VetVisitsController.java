package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.AppointmentService;
import tech.petclinix.logic.service.VisitService;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.web.dto.VetVisitRequest;
import tech.petclinix.web.dto.VetVisitResponse;

@RestController
@RequestMapping("/vet/visits")
public class VetVisitsController {

    private final AppointmentService appointmentService;
    private final VisitService visitService;

    public VetVisitsController(AppointmentService appointmentService, VisitService visitService) {
        this.appointmentService = appointmentService;
        this.visitService = visitService;
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<VetVisitResponse> get(Authentication authentication, @PathVariable Long appointmentId) {
        AppointmentEntity appointment = appointmentService.retrieveByVetAndId(new Username(authentication.getName()), appointmentId);
        VisitEntity visit = visitService.findOrCreateByAppointment(appointment);
        return ResponseEntity.ok(toResponse(visit));
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<VetVisitResponse> put(Authentication authentication, @PathVariable Long appointmentId,
                                                 @RequestBody VetVisitRequest request) {
        AppointmentEntity appointment = appointmentService.retrieveByVetAndId(new Username(authentication.getName()), appointmentId);
        VisitEntity visit = visitService.persist(appointment, request.vetSummary(), request.ownerSummary(), request.vaccination());
        return ResponseEntity.ok(toResponse(visit));
    }

    private VetVisitResponse toResponse(VisitEntity visit) {
        return new VetVisitResponse(visit.getId(), visit.getVetSummary(), visit.getOwnerSummary(), visit.getVaccination());
    }
}
