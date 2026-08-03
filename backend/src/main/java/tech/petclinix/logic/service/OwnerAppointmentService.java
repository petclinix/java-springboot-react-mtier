package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Appointment;
import tech.petclinix.logic.domain.AppointmentData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.LocationClosedAtRequestedTimeException;
import tech.petclinix.logic.service.mapper.EntityMapper;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.PetEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class OwnerAppointmentService {

    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final AppointmentService appointmentService;
    private final PetService petService;
    private final LocationService locationService;

    public OwnerAppointmentService(AppointmentService appointmentService, PetService petService, LocationService locationService) {
        this.appointmentService = appointmentService;
        this.petService = petService;
        this.locationService = locationService;
    }

    @Transactional(readOnly = true)
    public List<Appointment> findAllByOwner(Username ownerUsername) {
        return appointmentService.findAllByOwner(ownerUsername).stream()
                .map(EntityMapper::toAppointment)
                .toList();
    }

    @Transactional
    public Appointment persist(Username ownerUsername, AppointmentData appointmentData) {
        PetEntity pet = petService.retrieveByOwnerAndId(ownerUsername, appointmentData.petId());
        LocationEntity location = locationService.retrieveById(appointmentData.locationId());
        LocalDateTime startsAt = appointmentData.startsAt();
        LocalDateTime endsAt = startsAt.plusMinutes(DEFAULT_DURATION_MINUTES);
        assertLocationIsOpen(location, startsAt);
        AppointmentEntity persisted = appointmentService.persist(pet, location, startsAt, endsAt);
        return EntityMapper.toAppointment(persisted);
    }

    private void assertLocationIsOpen(LocationEntity location, LocalDateTime startsAt) {
        Instant instant = startsAt.atZone(ZoneId.of(location.getZoneId())).toInstant();
        if (!location.isOpenAt(instant)) {
            throw new LocationClosedAtRequestedTimeException(location.getName(), startsAt);
        }
    }

    @Transactional
    public void cancelByOwner(Username ownerUsername, Long id) {
        appointmentService.cancelByOwner(ownerUsername, id);
    }
}
