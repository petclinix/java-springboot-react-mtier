package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;

import tech.petclinix.persistence.jpa.AppointmentJpaRepository.Specifications;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

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

    public AppointmentEntity retrieveByVetAndId(String vetUsername, Long appointmentId) {
        return retrieveByIdAndSpec(appointmentId, Specifications.byVetUsername(vetUsername),
                 () -> "vet " + vetUsername + ", id " + appointmentId);
    }

    @Transactional
    public void cancelByOwner(String ownerUsername, Long appointmentId) {
        deleteBySpec(
                appointmentId, Specifications.byOwnerUsername(ownerUsername),
                () -> "owner " + ownerUsername + ", id " + appointmentId);
    }

    @Transactional
    public void cancelByVet(String vetUsername, Long appointmentId) {
        deleteBySpec(
                appointmentId, Specifications.byVetUsername(vetUsername),
                () -> "vet " + vetUsername + ", id " + appointmentId
        );
    }

    private void deleteBySpec(Long appointmentId, Specification<AppointmentEntity> spec, Supplier<String> notFoundContext) {
        var appointment = retrieveByIdAndSpec(appointmentId, spec, notFoundContext);
        repository.delete(appointment);
    }

    private AppointmentEntity retrieveByIdAndSpec(Long appointmentId, Specification<AppointmentEntity> spec, Supplier<String> notFoundContext) {
        return repository.findOne(Specifications.byId(appointmentId).and(spec))
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found: " + notFoundContext.get()));
    }

}
