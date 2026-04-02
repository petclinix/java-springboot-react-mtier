package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.NotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tech.petclinix.logic.domain.Username;
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

    /* default */ List<AppointmentEntity> findAllByOwner(Username ownerUsername) {
        return repository.findAll(Specifications.byOwnerUsername(ownerUsername));
    }

    /* default */ List<AppointmentEntity> findAllByVet(Username vetUsername) {
        return repository.findAll(Specifications.byVetUsername(vetUsername));
    }

    /* default */ AppointmentEntity retrieveByVetAndId(Username vetUsername, Long appointmentId) {
        return retrieveByIdAndSpec(appointmentId, Specifications.byVetUsername(vetUsername),
                () -> "vet %s, id %d".formatted(vetUsername.value(), appointmentId)
        );
    }

    /* default */ AppointmentEntity persist(PetEntity pet, VetEntity vet, LocalDateTime startAt) {
        var appointment = new AppointmentEntity(vet, pet, startAt);
        return repository.save(appointment);
    }

    /* default */ void cancelByOwner(Username ownerUsername, Long appointmentId) {
        deleteBySpec(
                appointmentId, Specifications.byOwnerUsername(ownerUsername),
                () -> "owner %s, id %d".formatted(ownerUsername.value(), appointmentId)
        );
    }

    /* default */ void cancelByVet(Username vetUsername, Long appointmentId) {
        deleteBySpec(
                appointmentId, Specifications.byVetUsername(vetUsername),
                () -> "vet %s, id %d".formatted(vetUsername.value(), appointmentId)
        );
    }

    private void deleteBySpec(Long appointmentId, Specification<AppointmentEntity> spec, Supplier<String> notFoundContext) {
        var appointment = retrieveByIdAndSpec(appointmentId, spec, notFoundContext);
        repository.delete(appointment);
    }

    private AppointmentEntity retrieveByIdAndSpec(Long appointmentId, Specification<AppointmentEntity> spec, Supplier<String> notFoundContext) {
        return repository.findOne(Specifications.byId(appointmentId).and(spec))
                .orElseThrow(() -> new NotFoundException("Appointment not found: %s".formatted(notFoundContext.get())));
    }

}
