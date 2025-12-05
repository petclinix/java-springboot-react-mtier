package tech.petclinix.logic.service;


import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.*;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.LocationJpaRepository.Specifications;
import tech.petclinix.persistence.mapper.PetMapper;
import tech.petclinix.web.dto.LocationResponse;

import javax.swing.text.html.parser.Entity;
import java.util.List;

@Service
public class LocationService {

    private final LocationJpaRepository repository;
    private final EntityManager entityManager;
    private final VetService vetService;

    public LocationService(LocationJpaRepository repository, EntityManager entityManager, VetService vetService) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.vetService = vetService;
    }

    public List<LocationEntity> findAllByVet(Authentication authentication) {
        VetEntity vet = vetService.retrieveByUsername(authentication.getName());

        return repository.findAll(Specifications.byVet(vet));
    }

    @Transactional
    public LocationEntity persist(Authentication authentication, LocationResponse request) {
        var vet = vetService.retrieveByUsername(authentication.getName());

        var location = new LocationEntity(vet, request.name(), request.zoneId());
        request.weeklyPeriods().stream()
                .map(period -> new OpeningPeriod(location, period.dayOfWeek(), period.startTime(), period.endTime(), period.sortOrder()))
                .forEach(entityManager::persist);

        request.exceptions().stream()
                .map(exception -> new OpeningException(location, exception.date(), exception.closed(), exception.note()))
                .forEach(entityManager::persist);

        return repository.save(location);
    }

}
