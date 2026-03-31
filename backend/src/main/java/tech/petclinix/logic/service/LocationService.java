package tech.petclinix.logic.service;


import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.*;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.LocationJpaRepository.Specifications;
import tech.petclinix.web.dto.LocationResponse;

import jakarta.persistence.EntityNotFoundException;

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

    public LocationEntity findByVetAndId(Authentication authentication, Long id) {
        VetEntity vet = vetService.retrieveByUsername(authentication.getName());
        LocationEntity location = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + id));
        if (!location.getVet().getId().equals(vet.getId())) {
            throw new EntityNotFoundException("Location not found: " + id);
        }
        return location;
    }

    public List<LocationEntity> findAllByVet(Authentication authentication) {
        VetEntity vet = vetService.retrieveByUsername(authentication.getName());

        return repository.findAll(Specifications.byVet(vet));
    }

    @Transactional
    public LocationEntity update(Authentication authentication, Long id, LocationResponse request) {
        LocationEntity location = findByVetAndId(authentication, id);

        location.setName(request.name());
        location.setZoneId(request.zoneId());

        location.getWeeklyPeriods().clear();
        location.getOverrides().clear();
        entityManager.flush(); // force DELETEs before INSERTs to avoid unique constraint violations

        if (request.weeklyPeriods() != null) {
            request.weeklyPeriods().stream()
                    .map(p -> new OpeningPeriod(location, p.dayOfWeek(), p.startTime(), p.endTime(), p.sortOrder()))
                    .forEach(location.getWeeklyPeriods()::add);
        }
        if (request.overrides() != null) {
            request.overrides().stream()
                    .map(o -> new OpeningOverride(location, o.date(), o.openTime(), o.closeTime(), o.closed(), o.reason()))
                    .forEach(location.getOverrides()::add);
        }

        return repository.save(location);
    }

    @Transactional
    public LocationEntity persist(Authentication authentication, LocationResponse request) {
        var vet = vetService.retrieveByUsername(authentication.getName());

        var location = new LocationEntity(vet, request.name(), request.zoneId());
        request.weeklyPeriods().stream()
                .map(period -> new OpeningPeriod(location, period.dayOfWeek(), period.startTime(), period.endTime(), period.sortOrder()))
                .forEach(entityManager::persist);

        if (request.overrides() != null)
            request.overrides().stream()
                    .map(exception -> new OpeningOverride(location, exception.date(), exception.openTime(), exception.closeTime(), exception.closed(), exception.reason()))
                    .forEach(entityManager::persist);

        return repository.save(location);
    }

}
