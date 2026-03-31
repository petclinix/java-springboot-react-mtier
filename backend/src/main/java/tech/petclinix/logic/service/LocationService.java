package tech.petclinix.logic.service;


import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.LocationData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.*;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.LocationJpaRepository.Specifications;

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

    public LocationEntity findByVetAndId(Username vetUsername, Long id) {
        VetEntity vet = vetService.retrieveByUsername(new Username(vetUsername.value()));
        LocationEntity location = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + id));
        if (!location.getVet().getId().equals(vet.getId())) {
            throw new EntityNotFoundException("Location not found: " + id);
        }
        return location;
    }

    public List<LocationEntity> findAllByVet(Username vetUsername) {
        VetEntity vet = vetService.retrieveByUsername(new Username(vetUsername.value()));
        return repository.findAll(Specifications.byVet(vet));
    }

    @Transactional
    public LocationEntity update(Username vetUsername, Long id, LocationData locationData) {
        LocationEntity location = findByVetAndId(vetUsername, id);

        location.setName(locationData.name());
        location.setZoneId(locationData.zoneId());

        location.getWeeklyPeriods().clear();
        location.getOverrides().clear();
        entityManager.flush(); // force DELETEs before INSERTs to avoid unique constraint violations

        if (locationData.weeklyPeriods() != null) {
            locationData.weeklyPeriods().stream()
                    .map(p -> new OpeningPeriodEntity(location, p.dayOfWeek(), p.startTime(), p.endTime(), p.sortOrder()))
                    .forEach(location.getWeeklyPeriods()::add);
        }
        if (locationData.overrides() != null) {
            locationData.overrides().stream()
                    .map(o -> new OpeningOverrideEntity(location, o.date(), o.openTime(), o.closeTime(), o.closed(), o.reason()))
                    .forEach(location.getOverrides()::add);
        }

        return repository.save(location);
    }

    @Transactional
    public LocationEntity persist(Username vetUsername, LocationData locationData) {
        var vet = vetService.retrieveByUsername(vetUsername);

        var location = new LocationEntity(vet, locationData.name(), locationData.zoneId());
        locationData.weeklyPeriods().stream()
                .map(period -> new OpeningPeriodEntity(location, period.dayOfWeek(), period.startTime(), period.endTime(), period.sortOrder()))
                .forEach(entityManager::persist);

        if (locationData.overrides() != null)
            locationData.overrides().stream()
                    .map(exception -> new OpeningOverrideEntity(location, exception.date(), exception.openTime(), exception.closeTime(), exception.closed(), exception.reason()))
                    .forEach(entityManager::persist);

        return repository.save(location);
    }

}
