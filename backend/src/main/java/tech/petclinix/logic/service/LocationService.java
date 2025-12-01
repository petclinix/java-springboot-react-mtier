package tech.petclinix.logic.service;


import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.LocationJpaRepository.Specifications;

import java.util.List;

@Service
public class LocationService {

    private final LocationJpaRepository repository;
    private final VetService vetService;

    public LocationService(LocationJpaRepository repository, VetService vetService) {
        this.repository = repository;
        this.vetService = vetService;
    }

    public List<LocationEntity> findAllByVet(Authentication authentication) {
        VetEntity vet = vetService.retrieveByUsername(authentication.getName());

        return repository.findAll(Specifications.byVet(vet));
    }
}
