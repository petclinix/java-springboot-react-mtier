package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Appointment;
import tech.petclinix.logic.domain.AppointmentData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.mapper.EntityMapper;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.util.List;

@Service
public class OwnerAppointmentService {

    private final AppointmentService appointmentService;
    private final PetService petService;
    private final VetService vetService;

    public OwnerAppointmentService(AppointmentService appointmentService, PetService petService, VetService vetService) {
        this.appointmentService = appointmentService;
        this.petService = petService;
        this.vetService = vetService;
    }

    public List<Appointment> findAllByOwner(Username ownerUsername) {
        return appointmentService.findAllByOwner(ownerUsername).stream()
                .map(EntityMapper::toAppointment)
                .toList();
    }

    @Transactional
    public Appointment persist(Username ownerUsername, AppointmentData appointmentData) {
        PetEntity pet = petService.retrieveByOwnerAndId(ownerUsername, appointmentData.petId());
        VetEntity vet = vetService.retrieveById(appointmentData.vetId());
        AppointmentEntity persisted = appointmentService.persist(pet, vet, appointmentData.startsAt());
        return EntityMapper.toAppointment(persisted);
    }

    @Transactional
    public void cancelByOwner(Username ownerUsername, Long id) {
        appointmentService.cancelByOwner(ownerUsername, id);
    }
}
