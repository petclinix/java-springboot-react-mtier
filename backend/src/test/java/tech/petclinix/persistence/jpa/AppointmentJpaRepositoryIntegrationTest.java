package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link AppointmentJpaRepository}.
 *
 * Verifies each Specification — {@code byVet}, {@code byPet}, {@code byOwnerUsername},
 * {@code byVetUsername}, {@code byId} — and the custom query {@code countPerVet()}
 * against H2.
 * Happy path only — no mocking, full JPA stack loaded via {@code @DataJpaTest}.
 */
@DataJpaTest
class AppointmentJpaRepositoryIntegrationTest {

    @Autowired
    AppointmentJpaRepository appointmentRepository;

    @Autowired
    VetJpaRepository vetRepository;

    @Autowired
    OwnerJpaRepository ownerRepository;

    @Autowired
    PetJpaRepository petRepository;

    private VetEntity vetMia;
    private VetEntity vetNick;
    private OwnerEntity ownerOlga;
    private PetEntity petBuddy;
    private PetEntity petMax;
    private AppointmentEntity appt1;
    private AppointmentEntity appt2;
    private AppointmentEntity appt3;

    @BeforeEach
    void setUp() {
        vetMia = vetRepository.save(new VetEntity("vet-mia", "hash1"));
        vetNick = vetRepository.save(new VetEntity("vet-nick", "hash2"));
        ownerOlga = ownerRepository.save(new OwnerEntity("owner-olga", "hash3"));
        petBuddy = petRepository.save(new PetEntity("Buddy", ownerOlga));
        petMax = petRepository.save(new PetEntity("Max", ownerOlga));
        appt1 = appointmentRepository.save(
                new AppointmentEntity(vetMia, petBuddy, LocalDateTime.of(2026, 4, 1, 9, 0)));
        appt2 = appointmentRepository.save(
                new AppointmentEntity(vetMia, petMax, LocalDateTime.of(2026, 4, 2, 10, 0)));
        appt3 = appointmentRepository.save(
                new AppointmentEntity(vetNick, petBuddy, LocalDateTime.of(2026, 4, 3, 11, 0)));
    }

    /** Returns all appointments for the given vet entity. */
    @Test
    void byVetFindsAllAppointmentsForThatVet() {
        //act
        var results = appointmentRepository.findAll(
                AppointmentJpaRepository.Specifications.byVet(vetMia));

        //assert
        assertThat(results).hasSize(2);
    }

    /** Returns all appointments for the given pet entity. */
    @Test
    void byPetFindsAllAppointmentsForThatPet() {
        //act
        var results = appointmentRepository.findAll(
                AppointmentJpaRepository.Specifications.byPet(petBuddy));

        //assert
        assertThat(results).hasSize(2);
    }

    /** Returns all appointments whose pet belongs to the owner with the given username. */
    @Test
    void byOwnerUsernameFindsAllAppointmentsForThatOwner() {
        //act
        var results = appointmentRepository.findAll(
                AppointmentJpaRepository.Specifications.byOwnerUsername(new Username("owner-olga")));

        //assert
        assertThat(results).hasSize(3);
    }

    /** Returns all appointments for the vet with the given username. */
    @Test
    void byVetUsernameFindsAllAppointmentsForThatVet() {
        //act
        var results = appointmentRepository.findAll(
                AppointmentJpaRepository.Specifications.byVetUsername(new Username("vet-nick")));

        //assert
        assertThat(results).hasSize(1);
    }

    /** Returns the appointment matching the given id. */
    @Test
    void byIdFindsMatchingAppointment() {
        //act
        var results = appointmentRepository.findAll(
                AppointmentJpaRepository.Specifications.byId(appt1.getId()));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(appt1.getId());
    }

    /** Returns per-vet appointment counts ordered by count descending. */
    @Test
    void countPerVetReturnsCountsGroupedByVetUsername() {
        //act
        var counts = appointmentRepository.countPerVet();

        //assert
        assertThat(counts).hasSize(2);
        // vetMia has 2 appointments — should appear first (descending order)
        assertThat(counts.get(0).vetUsername()).isEqualTo("vet-mia");
        assertThat(counts.get(0).count()).isEqualTo(2L);
        assertThat(counts.get(1).vetUsername()).isEqualTo("vet-nick");
        assertThat(counts.get(1).count()).isEqualTo(1L);
    }
}
