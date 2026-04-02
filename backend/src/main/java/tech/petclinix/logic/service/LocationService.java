package tech.petclinix.logic.service;


import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Location;
import tech.petclinix.logic.domain.LocationData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.mapper.LocationMapper;
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

    public Location findByVetAndId(Username vetUsername, Long id) {
        return LocationMapper.toLocation(findLocationEntityByVetAndId(vetUsername, id));
    }

    private LocationEntity findLocationEntityByVetAndId(Username vetUsername, Long id) {
        VetEntity vet = vetService.retrieveByUsername(vetUsername);
        LocationEntity location = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + id));
        if (!location.getVet().getId().equals(vet.getId())) {
            throw new EntityNotFoundException("Location not found: " + id);
        }
        return location;
    }

    public List<Location> findAllByVet(Username vetUsername) {
        VetEntity vet = vetService.retrieveByUsername(vetUsername);
        return repository.findAll(Specifications.byVet(vet)).stream()
                .map(LocationMapper::toLocation)
                .toList();
    }

    @Transactional
    public Location update(Username vetUsername, Long id, LocationData locationData) {
        LocationEntity locationEntity = findLocationEntityByVetAndId(vetUsername, id);

        locationEntity.setName(locationData.name());
        locationEntity.setZoneId(locationData.zoneId());

        locationEntity.getWeeklyPeriods().clear();
        locationEntity.getOverrides().clear();
        entityManager.flush(); // force DELETEs before INSERTs to avoid unique constraint violations

        if (locationData.weeklyPeriods() != null) {
            locationData.weeklyPeriods().stream()
                    .map(p -> new OpeningPeriodEntity(locationEntity, p.dayOfWeek(), p.startTime(), p.endTime(), p.sortOrder()))
                    .forEach(locationEntity.getWeeklyPeriods()::add);
        }
        if (locationData.overrides() != null) {
            locationData.overrides().stream()
                    .map(o -> new OpeningOverrideEntity(locationEntity, o.date(), o.openTime(), o.closeTime(), o.closed(), o.reason()))
                    .forEach(locationEntity.getOverrides()::add);
        }

        LocationEntity saved = repository.save(locationEntity);
        return LocationMapper.toLocation(saved);
    }

    @Transactional
    public Location persist(Username vetUsername, LocationData locationData) {
        var vet = vetService.retrieveByUsername(vetUsername);

        var locationEntity = new LocationEntity(vet, locationData.name(), locationData.zoneId());
        locationData.weeklyPeriods().stream()
                .map(period -> new OpeningPeriodEntity(locationEntity, period.dayOfWeek(), period.startTime(), period.endTime(), period.sortOrder()))
                .forEach(entityManager::persist);

        if (locationData.overrides() != null)
            locationData.overrides().stream()
                    .map(exception -> new OpeningOverrideEntity(locationEntity, exception.date(), exception.openTime(), exception.closeTime(), exception.closed(), exception.reason()))
                    .forEach(entityManager::persist);

        LocationEntity saved = repository.save(locationEntity);
        return LocationMapper.toLocation(saved);
    }

}
