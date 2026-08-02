package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.domain.exception.PetPictureTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.ActionEvent;
import tech.petclinix.logic.domain.Pet;
import tech.petclinix.logic.domain.PetData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.mapper.EntityMapper;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository.Specifications;
import java.util.List;

@Service
public class PetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PetService.class);

    private static final int MAX_PICTURE_BYTES = 2 * 1024 * 1024;

    private final PetJpaRepository repository;
    private final OwnerService ownerService;
    private final ApplicationEventPublisher eventPublisher;

    public PetService(PetJpaRepository repository, OwnerService ownerService, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.ownerService = ownerService;
        this.eventPublisher = eventPublisher;
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
                .orElseThrow(() -> new NotFoundException("Pet not found for owner " + ownerUsername.value() + " and pet id " + petId));
    }

    @Transactional
    public Pet persist(Username ownerUsername, PetData petData) {
        if (petData.picture() != null && petData.picture().length > MAX_PICTURE_BYTES) {
            throw new PetPictureTooLargeException(petData.picture().length, MAX_PICTURE_BYTES);
        }

        var owner = ownerService.retrieveByUsername(ownerUsername);

        var entity = new PetEntity(petData.name(), owner);
        entity.setSpecies(petData.species());
        entity.setBreed(petData.breed());
        entity.setGender(petData.gender());
        entity.setBirthDate(petData.birthDate());
        entity.setPicture(petData.picture());
        entity.setPictureContentType(petData.pictureContentType());
        PetEntity saved = repository.save(entity);
        eventPublisher.publishEvent(new ActionEvent(ownerUsername, "PET_CREATED"));
        LOGGER.info("Pet '{}' created for owner {}", saved.getName(), ownerUsername.value());
        return EntityMapper.toPet(saved);
    }

}
