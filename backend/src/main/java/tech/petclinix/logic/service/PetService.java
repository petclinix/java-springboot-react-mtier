package tech.petclinix.logic.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository.Specifications;
import tech.petclinix.persistence.mapper.PetMapper;

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

    public List<DomainPet> findAllByOwner(Authentication authentication) {
        var owner = ownerService.retrieveByUsername(authentication.getName());

        return repository.findAll(Specifications.byOwner(owner)).stream()
                .map(PetMapper::toDomain)
                .toList();
    }

    public Optional<DomainPet> findByName(String name) {
        return repository.findByName(name).map(PetMapper::toDomain);
    }


    @Transactional
    public DomainPet persist(String name, Authentication authentication) {
        var owner = ownerService.retrieveByUsername(authentication.getName());

        var entity = new PetEntity(name, owner);
        var saved = repository.save(entity);
        return PetMapper.toDomain(saved);
    }


}
