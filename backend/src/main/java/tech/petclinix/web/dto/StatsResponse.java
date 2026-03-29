package tech.petclinix.web.dto;

import java.util.List;

public record StatsResponse(
        long totalOwners,
        long totalVets,
        long totalPets,
        long totalAppointments,
        List<VetAppointmentCount> appointmentsPerVet
) {
}
