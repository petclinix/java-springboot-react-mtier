package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Pet;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.mapper.EntityMapper;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository.Specifications;
import java.util.List;

@Service
public class PetService {

    private final PetJpaRepository repository;
    private final OwnerService ownerService;

    public PetService(PetJpaRepository repository, OwnerService ownerService) {
        this.repository = repository;
        this.ownerService = ownerService;
    }

    @Transactional(readOnly = true)
    public List<Pet> findAllByOwner(Username ownerUsername) {
        var owner = ownerService.retrieveByUsername(ownerUsername);
        return repository.findAll(Specifications.byOwner(owner)).stream()
                .map(EntityMapper::toPet)
                .toList();
    }

    /* default */ PetEntity retrieveByOwnerAndId(Username ownerUsername, Long petId) {
        return repository.findOne(
                        Specifications.byOwnerUsername(ownerUsername)
                                .and(Specifications.byId(petId))
                )
                .orElseThrow(() -> new EntityNotFoundException("Pet not found for owner " + ownerUsername.value() + " and pet id " + petId));
    }

    @Transactional
    public Pet persist(Username ownerUsername, String name) {
        var owner = ownerService.retrieveByUsername(ownerUsername);

        var entity = new PetEntity(name, owner);
        PetEntity saved = repository.save(entity);
        return EntityMapper.toPet(saved);
    }

}
