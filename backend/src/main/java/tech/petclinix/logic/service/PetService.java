package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.DomainPet;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.mapper.PetMapper;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository.Specifications;

import java.util.List;
import java.util.Optional;

@Service
public class PetService {

    private final PetJpaRepository repository;
    private final OwnerService ownerService;

    public PetService(PetJpaRepository repository, OwnerService ownerService) {
        this.repository = repository;
        this.ownerService = ownerService;
    }

    public List<DomainPet> findAllByOwner(Username username) {
        var owner = ownerService.retrieveByUsername(username);

        return repository.findAll(Specifications.byOwner(owner)).stream()
                .map(PetMapper::toDomain)
                .toList();
    }

    public PetEntity retrieveByOwnerAndId(Username ownerUsername, Long petId) {
        return repository.findOne(
                        Specifications.byOwnerUsername(ownerUsername)
                                .and(Specifications.byId(petId))
                )
                .orElseThrow(() -> new EntityNotFoundException("Pet not found for owner " + ownerUsername.value() + " and pet id " + petId));
    }

    public Optional<DomainPet> findByName(String name) {
        return repository.findByName(name)
                .map(PetMapper::toDomain);
    }


    @Transactional
    public DomainPet persist(Username ownerUsername, String name) {
        var owner = ownerService.retrieveByUsername(ownerUsername);

        var entity = new PetEntity(name, owner);
        var saved = repository.save(entity);
        return PetMapper.toDomain(saved);
    }


}
