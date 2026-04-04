package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.jpa.VisitJpaRepository;
import tech.petclinix.persistence.jpa.VisitJpaRepository.Specifications;

import java.util.List;

@Service
public class VisitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisitService.class);

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
        var existing = repository.findOne(Specifications.byAppointment(appointment));
        VisitEntity visit = existing.orElseGet(() -> new VisitEntity(appointment));

        visit.setVetSummary(vetSummary);
        visit.setOwnerSummary(ownerSummary);
        visit.setVaccination(vaccination);
        var saved = repository.save(visit);
        LOGGER.info("Visit {} for appointment {}", existing.isPresent() ? "updated" : "created", appointment.getId());
        return saved;
    }
}
