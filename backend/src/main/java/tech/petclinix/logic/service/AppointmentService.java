package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;

import tech.petclinix.persistence.jpa.AppointmentJpaRepository.Specifications;

import java.time.LocalDateTime;
import java.util.List;

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

    public List<AppointmentEntity> findAllByOwner(String ownerUsername) {
        return repository.findAll(Specifications.byOwnerUsername(ownerUsername));
    }

    public List<AppointmentEntity> findAllByVet(String vetUsername) {
        return repository.findAll(Specifications.byVetUsername(vetUsername));
    }

    @Transactional
    public void cancel(String ownerUsername, Long appointmentId) {
        var appointment = repository.findOne(
                Specifications.byOwnerUsername(ownerUsername)
                        .and(Specifications.byId(appointmentId))
        ).orElseThrow(() -> new EntityNotFoundException("Appointment not found for owner " + ownerUsername + " and id " + appointmentId));
        repository.delete(appointment);
    }

    @Transactional
    public void cancelByVet(String vetUsername, Long appointmentId) {
        var appointment = repository.findOne(
                Specifications.byVetUsername(vetUsername)
                        .and(Specifications.byId(appointmentId))
        ).orElseThrow(() -> new EntityNotFoundException("Appointment not found for vet " + vetUsername + " and id " + appointmentId));
        repository.delete(appointment);
    }

}
