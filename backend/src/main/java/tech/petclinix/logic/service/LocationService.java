package tech.petclinix.logic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Location;
import tech.petclinix.logic.domain.LocationData;
import tech.petclinix.logic.domain.LocationData.OverrideData;
import tech.petclinix.logic.domain.LocationData.PeriodData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.mapper.LocationMapper;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.OpeningOverrideEntity;
import tech.petclinix.persistence.entity.OpeningPeriodEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.LocationJpaRepository.Specifications;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationService.class);

    private final LocationJpaRepository repository;
    private final VetService vetService;

    public LocationService(LocationJpaRepository repository, VetService vetService) {
        this.repository = repository;
        this.vetService = vetService;
    }

    @Transactional(readOnly = true)
    public Location findByVetAndId(Username vetUsername, Long id) {
        return LocationMapper.toLocation(findLocationEntityByVetAndId(vetUsername, id));
    }

    @Transactional(readOnly = true)
    public List<Location> findAllByVet(Username vetUsername) {
        VetEntity vet = vetService.retrieveByUsername(vetUsername);
        return repository.findAll(Specifications.byVet(vet)).stream()
                .map(LocationMapper::toLocation)
                .toList();
    }

    @Transactional
    public Location persist(Username vetUsername, LocationData locationData) {
        VetEntity vet = vetService.retrieveByUsername(vetUsername);
        LocationEntity entity = new LocationEntity(vet, locationData.name(), locationData.zoneId());
        applyLocationData(entity, locationData);
        LocationEntity saved = repository.save(entity);
        LOGGER.info("Location '{}' created for vet {}", saved.getName(), vetUsername.value());
        return LocationMapper.toLocation(saved);
    }

    @Transactional
    public void delete(Username vetUsername, Long id) {
        LocationEntity entity = findLocationEntityByVetAndId(vetUsername, id);
        repository.delete(entity);
        LOGGER.info("Location {} deleted by vet {}", id, vetUsername.value());
    }

    @Transactional
    public Location update(Username vetUsername, Long id, LocationData locationData) {
        LocationEntity entity = findLocationEntityByVetAndId(vetUsername, id);
        entity.setName(locationData.name());
        entity.setZoneId(locationData.zoneId());
        applyLocationData(entity, locationData);
        LocationEntity saved = repository.save(entity);
        LOGGER.info("Location {} updated by vet {}", id, vetUsername.value());
        return LocationMapper.toLocation(saved);
    }

    /**
     * Syncs the period and override collections of {@code entity} against {@code data}
     * using a diff-based approach: matching items are updated in place, new items are
     * added, and removed items are deleted via orphan removal. No flush() required.
     *
     * <p>Natural keys: {@code (dayOfWeek, sortOrder)} for periods; {@code date} for overrides.
     */
    private void applyLocationData(LocationEntity entity, LocationData data) {
        syncPeriods(entity, data.weeklyPeriods() != null ? data.weeklyPeriods() : List.of());
        syncOverrides(entity, data.overrides() != null ? data.overrides() : List.of());
    }

    private void syncPeriods(LocationEntity entity, List<? extends PeriodData> incoming) {
        Map<String, OpeningPeriodEntity> existing = entity.getWeeklyPeriods().stream()
                .collect(Collectors.toMap(
                        p -> p.getDayOfWeek() + ":" + p.getSortOrder(),
                        p -> p));

        Set<String> incomingKeys = incoming.stream()
                .map(p -> p.dayOfWeek() + ":" + p.sortOrder())
                .collect(Collectors.toSet());

        for (var p : incoming) {
            String key = p.dayOfWeek() + ":" + p.sortOrder();
            if (existing.containsKey(key)) {
                OpeningPeriodEntity period = existing.get(key);
                period.setStartTime(p.startTime());
                period.setEndTime(p.endTime());
            } else {
                entity.getWeeklyPeriods().add(
                        new OpeningPeriodEntity(entity, p.dayOfWeek(), p.startTime(), p.endTime(), p.sortOrder()));
            }
        }

        entity.getWeeklyPeriods().removeIf(p -> !incomingKeys.contains(p.getDayOfWeek() + ":" + p.getSortOrder()));
    }

    private void syncOverrides(LocationEntity entity, List<? extends OverrideData> incoming) {
        Map<LocalDate, OpeningOverrideEntity> existing = entity.getOverrides().stream()
                .collect(Collectors.toMap(OpeningOverrideEntity::getDate, o -> o));

        Set<LocalDate> incomingDates = incoming.stream()
                .map(OverrideData::date)
                .collect(Collectors.toSet());

        for (var o : incoming) {
            if (existing.containsKey(o.date())) {
                OpeningOverrideEntity override = existing.get(o.date());
                override.setOpenTime(o.openTime());
                override.setCloseTime(o.closeTime());
                override.setClosed(o.closed());
                override.setReason(o.reason());
            } else {
                entity.getOverrides().add(
                        new OpeningOverrideEntity(entity, o.date(), o.openTime(), o.closeTime(), o.closed(), o.reason()));
            }
        }

        entity.getOverrides().removeIf(o -> !incomingDates.contains(o.getDate()));
    }

    private LocationEntity findLocationEntityByVetAndId(Username vetUsername, Long id) {
        VetEntity vet = vetService.retrieveByUsername(vetUsername);
        LocationEntity location = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found: " + id));
        if (!location.getVet().getId().equals(vet.getId())) {
            throw new NotFoundException("Location not found: " + id);
        }
        return location;
    }
}
