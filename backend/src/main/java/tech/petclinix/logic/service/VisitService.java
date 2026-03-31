package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
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

    public VisitEntity retrieveByAppointment(AppointmentEntity appointment) {
        return repository.findByAppointment(appointment)
                .orElseThrow(() -> new EntityNotFoundException("Visit not found for appointment " + appointment.getId()));
    }

    public List<VisitEntity> findAllByPet(PetEntity pet) {
        return repository.findAllByAppointment_Pet(pet);
    }

    public VisitEntity persist(AppointmentEntity appointment, String vetSummary, String ownerSummary, String vaccination) {
        VisitEntity visit = repository.findByAppointment(appointment)
                .orElseGet(() -> new VisitEntity(appointment));

        visit.setVetSummary(vetSummary);
        visit.setOwnerSummary(ownerSummary);
        visit.setVaccination(vaccination);
        return repository.save(visit);
    }
}
