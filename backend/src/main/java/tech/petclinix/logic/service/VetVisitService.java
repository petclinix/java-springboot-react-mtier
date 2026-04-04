package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.VetVisit;
import tech.petclinix.logic.domain.VetVisitData;
import tech.petclinix.logic.service.mapper.EntityMapper;
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

    @Transactional(readOnly = true)
    public VetVisit retrieveByVetAndId(Username vetUsername, Long appointmentId) {
        AppointmentEntity appointment = appointmentService.retrieveByVetAndId(vetUsername, appointmentId);
        return EntityMapper.toVetVisit(visitService.retrieveByAppointment(appointment));
    }

    @Transactional
    public VetVisit persist(Username vetUsername, Long appointmentId, VetVisitData vetVisitData) {
        AppointmentEntity appointment = appointmentService.retrieveByVetAndId(vetUsername, appointmentId);
        VisitEntity persisted = visitService.persist(appointment, vetVisitData.vetSummary(), vetVisitData.ownerSummary(), vetVisitData.vaccination());
        return EntityMapper.toVetVisit(persisted);
    }

}
