package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.jpa.VisitJpaRepository;

import java.util.List;

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

    public List<VisitEntity> findAllByPet(PetEntity pet) {
        return repository.findAllByAppointment_Pet(pet);
    }

    @Transactional
    public VisitEntity persist(AppointmentEntity appointment, String vetSummary, String ownerSummary, String vaccination) {
        VisitEntity visit = findOrCreateByAppointment(appointment);

        visit.setVetSummary(vetSummary);
        visit.setOwnerSummary(ownerSummary);
        visit.setVaccination(vaccination);
        return repository.save(visit);
    }
}
