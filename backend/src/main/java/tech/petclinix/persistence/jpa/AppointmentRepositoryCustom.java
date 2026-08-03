package tech.petclinix.persistence.jpa;

import tech.petclinix.logic.domain.StatsData.VetAppointmentCount;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepositoryCustom {

    List<VetAppointmentCount> countPerVet();

    List<AppointmentEntity> findOverlappingForUpdate(VetEntity vet, LocalDateTime startAt, LocalDateTime endsAt);
}
