package tech.petclinix.logic.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.VetVisitData;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.jpa.VisitJpaRepository;

import java.util.List;

@Service
public class VisitService {

    private final VisitJpaRepository repository;
    private final AppointmentService appointmentService;

    public VisitService(VisitJpaRepository repository, AppointmentService appointmentService) {
        this.repository = repository;
        this.appointmentService = appointmentService;
    }

    public VisitEntity retrieveVisit(Username vetUsername, Long appointmentId) {
        AppointmentEntity appointment = appointmentService.retrieveByVetAndId(vetUsername, appointmentId);
        return findOrCreateByAppointment(appointment);
    }

    @Transactional
    public VisitEntity findOrCreateByAppointment(AppointmentEntity appointment) {
        return repository.findByAppointment(appointment)
                .orElseGet(() -> repository.save(new VisitEntity(appointment)));
    }

    public List<VisitEntity> findAllByPet(PetEntity pet) {
        return repository.findAllByAppointment_Pet(pet);
    }

    @Transactional
    public VisitEntity persist(Username vetUsername, Long appointmentId, VetVisitData vetVisitData) {
        AppointmentEntity appointment = appointmentService.retrieveByVetAndId(vetUsername, appointmentId);
        return persist(appointment, vetVisitData.vetSummary(), vetVisitData.ownerSummary(), vetVisitData.vaccination());
    }


    private VisitEntity persist(AppointmentEntity appointment, String vetSummary, String ownerSummary, String vaccination) {
        VisitEntity visit = findOrCreateByAppointment(appointment);

        visit.setVetSummary(vetSummary);
        visit.setOwnerSummary(ownerSummary);
        visit.setVaccination(vaccination);
        return repository.save(visit);
    }
}
