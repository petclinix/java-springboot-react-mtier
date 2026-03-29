package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;
import tech.petclinix.web.dto.StatsResponse;
import tech.petclinix.web.dto.VetAppointmentCount;

import java.util.List;

@Service
public class StatsService {

    private final OwnerJpaRepository ownerJpaRepository;
    private final VetJpaRepository vetJpaRepository;
    private final PetJpaRepository petJpaRepository;
    private final AppointmentJpaRepository appointmentJpaRepository;

    public StatsService(OwnerJpaRepository ownerJpaRepository,
                        VetJpaRepository vetJpaRepository,
                        PetJpaRepository petJpaRepository,
                        AppointmentJpaRepository appointmentJpaRepository) {
        this.ownerJpaRepository = ownerJpaRepository;
        this.vetJpaRepository = vetJpaRepository;
        this.petJpaRepository = petJpaRepository;
        this.appointmentJpaRepository = appointmentJpaRepository;
    }

    public StatsResponse getStats() {
        long totalOwners = ownerJpaRepository.count();
        long totalVets = vetJpaRepository.count();
        long totalPets = petJpaRepository.count();
        long totalAppointments = appointmentJpaRepository.count();

        List<VetAppointmentCount> appointmentsPerVet = appointmentJpaRepository.countByVetUsername()
                .stream()
                .map(row -> new VetAppointmentCount((String) row[0], (Long) row[1]))
                .toList();

        return new StatsResponse(totalOwners, totalVets, totalPets, totalAppointments, appointmentsPerVet);
    }
}
