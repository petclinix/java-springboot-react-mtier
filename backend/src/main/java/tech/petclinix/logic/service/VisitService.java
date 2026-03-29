package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.jpa.VisitJpaRepository;

@Service
public class VisitService {

    private final VisitJpaRepository repository;

    public VisitService(VisitJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public VisitEntity findOrCreateByAppointment(AppointmentEntity appointment) {
        return repository.findByAppointment(appointment)
                .orElseGet(() -> repository.save(new VisitEntity(appointment)));
    }

    @Transactional
    public VisitEntity saveVetFields(VisitEntity visit, String vetSummary, String vaccination) {
        visit.setVetSummary(vetSummary);
        visit.setVaccination(vaccination);
        return repository.save(visit);
    }
}
