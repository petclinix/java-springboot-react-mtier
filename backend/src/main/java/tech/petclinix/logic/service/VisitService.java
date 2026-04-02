package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.jpa.VisitJpaRepository;
import tech.petclinix.persistence.jpa.VisitJpaRepository.Specifications;

import java.util.List;

@Service
public class VisitService {

    private final VisitJpaRepository repository;

    public VisitService(VisitJpaRepository repository) {
        this.repository = repository;
    }

    /* default */ VisitEntity retrieveByAppointment(AppointmentEntity appointment) {
        return repository.findOne(Specifications.byAppointment(appointment))
                .orElseThrow(() -> new NotFoundException("Visit not found for appointment " + appointment.getId()));
    }

    public List<VisitEntity> findAllByPet(PetEntity pet) {
        return repository.findAll(Specifications.byPet(pet));
    }

    /* default */ VisitEntity persist(AppointmentEntity appointment, String vetSummary, String ownerSummary, String vaccination) {
        VisitEntity visit = repository.findOne(Specifications.byAppointment(appointment))
                .orElseGet(() -> new VisitEntity(appointment));

        visit.setVetSummary(vetSummary);
        visit.setOwnerSummary(ownerSummary);
        visit.setVaccination(vaccination);
        return repository.save(visit);
    }
}
