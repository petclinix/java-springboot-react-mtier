package tech.petclinix.web.controller.mapper;

import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.web.dto.*;

import java.time.LocalDate;

public class DtoMapper {

    public static UserResponse toUserResponse(DomainUser user) {
        return new UserResponse(user.id(), user.username(), user.userType() == UserType.OWNER);
    }

    public static VetResponse toVetResponse(VetEntity vet) {
        return new VetResponse(vet.getId(), vet.getUsername());
    }

    public static VetAppointmentResponse toVetAppointmentResponse(AppointmentEntity a) {
        return new VetAppointmentResponse(
                a.getId(),
                a.getPet().getId(),
                a.getPet().getName(),
                a.getPet().getOwner().getUsername(),
                a.getStartAt());
    }

    public static VetVisitResponse toVetVisitResponse(VisitEntity visit) {
        return new VetVisitResponse(visit.getId(), visit.getVetSummary(), visit.getOwnerSummary(), visit.getVaccination());
    }

    public static PetResponse toPetResponse(PetEntity pet) {
        return new PetResponse(pet.getId(), pet.getName(), "", "", LocalDate.now());
    }

    public static OwnerVisitResponse toOwnerVisitResponse(VisitEntity v) {
        return new OwnerVisitResponse(
                v.getId(),
                v.getOwnerSummary(),
                v.getVaccination(),
                v.getAppointment().getVet().getUsername(),
                v.getAppointment().getStartAt()
        );
    }

    public static AppointmentResponse toAppointmentResponse(AppointmentEntity a) {
        return new AppointmentResponse(a.getId(), a.getVet().getId(), a.getPet().getId(), a.getStartAt());
    }

    public static AdminUserResponse toAdminUserResponse(DomainUser user) {
        return new AdminUserResponse(
                user.id(),
                user.username(),
                user.userType().name(),
                user.active());
    }
}
