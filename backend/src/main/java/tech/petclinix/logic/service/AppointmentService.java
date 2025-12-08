package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;

import java.time.LocalDateTime;

@Service
public class AppointmentService {

    private final AppointmentJpaRepository repository;

    public AppointmentService(AppointmentJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AppointmentEntity persist(PetEntity pet, VetEntity vet, LocalDateTime startAt) {

        var entity = new AppointmentEntity(vet, pet, startAt);
        return repository.save(entity);
    }

}
