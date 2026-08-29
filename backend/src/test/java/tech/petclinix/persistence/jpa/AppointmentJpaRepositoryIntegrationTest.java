package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.logic.domain.AppointmentType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link AppointmentJpaRepository}.
 *
 * Verifies each Specification — {@code byVet}, {@code byPet}, {@code byOwnerUsername},
 * {@code byVetUsername}, {@code byId}, {@code active} — and the custom queries
 * {@code countPerVet()} and {@code findOverlappingForUpdate()} against H2.
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

    @Autowired
    LocationJpaRepository locationRepository;

    private VetEntity vetMia;
    private VetEntity vetNick;
    private OwnerEntity ownerOlga;
    private PetEntity petBuddy;
    private PetEntity petMax;
    private LocationEntity locationMia;
    private LocationEntity locationNick;
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
        locationMia = locationRepository.save(new LocationEntity(vetMia, "Mia's Clinic", "UTC"));
        locationNick = locationRepository.save(new LocationEntity(vetNick, "Nick's Clinic", "UTC"));

        appt1 = appointmentRepository.save(
                new AppointmentEntity(locationMia, petBuddy, LocalDateTime.of(2026, 4, 1, 9, 0), LocalDateTime.of(2026, 4, 1, 9, 30), AppointmentType.CHECKUP));
        appt2 = appointmentRepository.save(
                new AppointmentEntity(locationMia, petMax, LocalDateTime.of(2026, 4, 2, 10, 0), LocalDateTime.of(2026, 4, 2, 10, 30), AppointmentType.CHECKUP));
        appt3 = appointmentRepository.save(
                new AppointmentEntity(locationNick, petBuddy, LocalDateTime.of(2026, 4, 3, 11, 0), LocalDateTime.of(2026, 4, 3, 11, 30), AppointmentType.CHECKUP));
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

    /** Excludes cancelled appointments but keeps BOOKED ones. */
    @Test
    void activeExcludesCancelledAppointments() {
        //arrange
        appt1.cancel();
        appointmentRepository.save(appt1);

        //act
        var results = appointmentRepository.findAll(
                AppointmentJpaRepository.Specifications.byVet(vetMia)
                        .and(AppointmentJpaRepository.Specifications.active()));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(appt2.getId());
    }

    /** Includes CONFIRMED appointments, since they have not yet reached a terminal state. */
    @Test
    void activeIncludesConfirmedAppointments() {
        //arrange
        appt1.confirm();
        appointmentRepository.save(appt1);

        //act
        var results = appointmentRepository.findAll(
                AppointmentJpaRepository.Specifications.byVet(vetMia)
                        .and(AppointmentJpaRepository.Specifications.active()));

        //assert
        assertThat(results).hasSize(2);
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

    /** Returns the booked appointment that overlaps the requested window for the given vet. */
    @Test
    void findOverlappingForUpdateReturnsOverlappingBookedAppointment() {
        //act
        var results = appointmentRepository.findOverlappingForUpdate(
                vetMia, LocalDateTime.of(2026, 4, 1, 9, 15), LocalDateTime.of(2026, 4, 1, 9, 45));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(appt1.getId());
    }

    /** Returns an empty list when the requested window does not overlap any existing appointment for the vet. */
    @Test
    void findOverlappingForUpdateReturnsEmptyWhenNoOverlap() {
        //act
        var results = appointmentRepository.findOverlappingForUpdate(
                vetMia, LocalDateTime.of(2026, 4, 1, 10, 0), LocalDateTime.of(2026, 4, 1, 10, 30));

        //assert
        assertThat(results).isEmpty();
    }

    /**
     * Regression test: a CONFIRMED appointment must block an overlapping new booking just like a
     * BOOKED one — {@code findOverlappingForUpdate} used to filter on {@code status == BOOKED} only,
     * which let a vet be double-booked once one of the conflicting appointments was confirmed.
     */
    @Test
    void findOverlappingForUpdateReturnsOverlappingConfirmedAppointment() {
        //arrange
        appt1.confirm();
        appointmentRepository.save(appt1);

        //act
        var results = appointmentRepository.findOverlappingForUpdate(
                vetMia, LocalDateTime.of(2026, 4, 1, 9, 15), LocalDateTime.of(2026, 4, 1, 9, 45));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(appt1.getId());
    }

    /**
     * A longer-duration candidate window (as produced by a SURGERY-type booking) correctly
     * overlaps an existing appointment that a shorter, default-duration window would have missed
     * entirely — proving the overlap check is a real time-range comparison, not tied to a fixed
     * slot length.
     */
    @Test
    void findOverlappingForUpdateDetectsOverlapOnlyVisibleWithLongerDurationWindow() {
        //arrange — appt1 occupies 2026-04-01 09:00-09:30. A candidate starting at 08:45 with a
        // short 15-minute (VACCINATION) duration ends at 09:00 and does NOT overlap appt1, but the
        // same start time with a 60-minute (SURGERY) duration ends at 09:45 and DOES overlap it.
        var candidateStart = LocalDateTime.of(2026, 4, 1, 8, 45);
        var shortCandidateEnd = candidateStart.plusMinutes(15);
        var longCandidateEnd = candidateStart.plusMinutes(60);

        //act
        var shortWindowResults = appointmentRepository.findOverlappingForUpdate(vetMia, candidateStart, shortCandidateEnd);
        var longWindowResults = appointmentRepository.findOverlappingForUpdate(vetMia, candidateStart, longCandidateEnd);

        //assert
        assertThat(shortWindowResults).isEmpty();
        assertThat(longWindowResults).hasSize(1);
        assertThat(longWindowResults.get(0).getId()).isEqualTo(appt1.getId());
    }

    /** A cancelled appointment does not block booking of a new appointment in the same slot. */
    @Test
    void findOverlappingForUpdateIgnoresCancelledAppointments() {
        //arrange
        appt1.cancel();
        appointmentRepository.save(appt1);

        //act
        var results = appointmentRepository.findOverlappingForUpdate(
                vetMia, LocalDateTime.of(2026, 4, 1, 9, 0), LocalDateTime.of(2026, 4, 1, 9, 30));

        //assert
        assertThat(results).isEmpty();
    }
}
