package tech.petclinix.persistence.jpa;

import tech.petclinix.logic.domain.StatsData.VetAppointmentCount;

import java.util.List;

public interface AppointmentRepositoryCustom {

    List<VetAppointmentCount> countPerVet();
}
