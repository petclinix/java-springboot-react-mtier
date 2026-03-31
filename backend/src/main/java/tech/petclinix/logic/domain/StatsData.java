package tech.petclinix.logic.domain;

import java.util.List;

public record StatsData(
        long totalOwners,
        long totalVets,
        long totalPets,
        long totalAppointments,
        List<VetAppointmentCount> appointmentsPerVet
) {
    public record VetAppointmentCount(String vetUsername, long count) {
    }

}
