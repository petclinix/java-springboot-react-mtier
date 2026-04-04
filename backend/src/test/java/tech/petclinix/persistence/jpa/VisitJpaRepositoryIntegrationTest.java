package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VisitEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link VisitJpaRepository}.
 *
 * Verifies the {@code Specifications.byAppointment} and {@code Specifications.byPet}
 * queries execute correctly against H2.
 * Happy path only — no mocking, full JPA stack loaded via {@code @DataJpaTest}.
 */
@DataJpaTest
class VisitJpaRepositoryIntegrationTest {

    @Autowired
    VisitJpaRepository visitRepository;

    @Autowired
    AppointmentJpaRepository appointmentRepository;

    @Autowired
    VetJpaRepository vetRepository;

    @Autowired
    OwnerJpaRepository ownerRepository;

    @Autowired
    PetJpaRepository petRepository;

    private AppointmentEntity apptRex;
    private AppointmentEntity apptBella;
    private PetEntity petRex;
    private VisitEntity visitForRex;
    private VisitEntity visitForBella;

    @BeforeEach
    void setUp() {
        var vet = vetRepository.save(new VetEntity("vet-petra", "hash1"));
        var owner = ownerRepository.save(new OwnerEntity("owner-quinn", "hash2"));
        petRex = petRepository.save(new PetEntity("Rex", owner));
        var petBella = petRepository.save(new PetEntity("Bella", owner));
        apptRex = appointmentRepository.save(
                new AppointmentEntity(vet, petRex, LocalDateTime.of(2026, 5, 1, 9, 0)));
        apptBella = appointmentRepository.save(
                new AppointmentEntity(vet, petBella, LocalDateTime.of(2026, 5, 2, 10, 0)));
        visitForRex = visitRepository.save(new VisitEntity(apptRex));
        visitForBella = visitRepository.save(new VisitEntity(apptBella));
    }

    /** Returns the visit linked to the given appointment. */
    @Test
    void byAppointmentFindsVisitForThatAppointment() {
        //act
        var results = visitRepository.findAll(
                VisitJpaRepository.Specifications.byAppointment(apptRex));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(visitForRex.getId());
    }

    /** Returns all visits whose appointment is associated with the given pet. */
    @Test
    void byPetFindsAllVisitsForThatPet() {
        //act
        var results = visitRepository.findAll(
                VisitJpaRepository.Specifications.byPet(petRex));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(visitForRex.getId());
    }

    /** Returns an empty list when no visit exists for the given appointment. */
    @Test
    void byAppointmentReturnsEmptyListWhenNoVisitExists() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-ruth", "hash3"));
        var owner = ownerRepository.save(new OwnerEntity("owner-sam", "hash4"));
        var pet = petRepository.save(new PetEntity("Charlie", owner));
        var apptNoVisit = appointmentRepository.save(
                new AppointmentEntity(vet, pet, LocalDateTime.of(2026, 6, 1, 9, 0)));

        //act
        var results = visitRepository.findAll(
                VisitJpaRepository.Specifications.byAppointment(apptNoVisit));

        //assert
        assertThat(results).isEmpty();
    }
}
