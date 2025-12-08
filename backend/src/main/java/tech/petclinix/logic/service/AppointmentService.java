package tech.petclinix.logic.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository.Specifications;
import tech.petclinix.persistence.mapper.PetMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentJpaRepository repository;
    private final VetService vetService;
    private final PetService petService;

    public AppointmentService(AppointmentJpaRepository repository, VetService vetService, PetService petService) {
        this.repository = repository;
        this.vetService = vetService;
        this.petService = petService;
    }

    @Transactional
    public AppointmentEntity persist(Long vetId, Long petId, LocalDateTime startAt, Authentication authentication) {
        var vet = vetService.retrieveById(vetId);
        var pet = petService.retrieveByOwnerAndId(authentication.getName(), petId);

        var entity = new AppointmentEntity(vet, pet, startAt);
        return repository.save(entity);
    }

}
