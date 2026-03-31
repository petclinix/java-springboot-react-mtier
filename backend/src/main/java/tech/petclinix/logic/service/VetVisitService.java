package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.VetVisitData;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.VisitEntity;

@Service
public class VetVisitService {
    private final AppointmentService appointmentService;
    private final VisitService visitService;

    public VetVisitService(AppointmentService appointmentService, VisitService visitService) {
        this.appointmentService = appointmentService;
        this.visitService = visitService;
    }

    public VisitEntity retrieveVisit(Username vetUsername, Long appointmentId) {
        AppointmentEntity appointment = appointmentService.retrieveByVetAndId(vetUsername, appointmentId);
        return visitService.retrieveByAppointment(appointment);
    }

    @Transactional
    public VisitEntity persist(Username vetUsername, Long appointmentId, VetVisitData vetVisitData) {
        AppointmentEntity appointment = appointmentService.retrieveByVetAndId(vetUsername, appointmentId);
        return visitService.persist(appointment, vetVisitData.vetSummary(), vetVisitData.ownerSummary(), vetVisitData.vaccination());
    }

}
