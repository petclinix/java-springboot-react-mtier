package tech.petclinix.logic.service.mapper;

import tech.petclinix.logic.domain.*;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VisitEntity;

import java.time.LocalDate;

public class EntityMapper {

    public static Pet toPet(PetEntity pet) {
        return new Pet(
                pet.getId(),
                pet.getName(),
                pet.getSpecies() != null ? pet.getSpecies().name() : null,
                pet.getGender()  != null ? pet.getGender().name()  : null,
                pet.getBirthDate()
        );
    }


    public static OwnerVisit toOwnerVisit(VisitEntity v) {
        return new OwnerVisit(
                v.getId(),
                v.getOwnerSummary(),
                v.getVaccination(),
                v.getAppointment().getVet().getUsername(),
                v.getAppointment().getStartAt()
        );
    }

    public static Appointment toAppointment(AppointmentEntity a) {
        return new Appointment(
                a.getId(),
                a.getVet().getId(),
                a.getPet().getId(),
                a.getStartAt()
        );
    }

    public static Vet toVet(VetEntity vet) {
        return new Vet(vet.getId(), vet.getUsername());
    }


    public static VetAppointment toVetAppointment(AppointmentEntity a) {
        return new VetAppointment(
                a.getId(),
                a.getPet().getId(),
                a.getPet().getName(),
                a.getPet().getOwner().getUsername(),
                a.getStartAt());
    }

    public static VetVisit toVetVisit(VisitEntity visit) {
        return new VetVisit(visit.getId(), visit.getVetSummary(), visit.getOwnerSummary(), visit.getVaccination());
    }


}
