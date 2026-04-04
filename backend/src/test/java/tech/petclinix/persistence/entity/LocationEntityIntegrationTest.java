package tech.petclinix.persistence.entity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity relation test for {@link LocationEntity}.
 *
 * Verifies that the {@code @OneToMany(cascade = ALL, orphanRemoval = true)} declarations
 * on {@code weeklyPeriods} and {@code overrides} behave correctly against H2.
 * Uses {@code @DataJpaTest} — no services, no mocking.
 */
@DataJpaTest
class LocationEntityIntegrationTest {

    @Autowired
    private VetJpaRepository vetRepository;

    @Autowired
    private LocationJpaRepository locationRepository;

    @Autowired
    private EntityManager entityManager;

    /** Saving a location cascades to its weekly periods. */
    @Test
    void savingLocationCascadesToWeeklyPeriods() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-jack", "hash"));
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");
        var period = new OpeningPeriodEntity(location, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 0);
        location.getWeeklyPeriods().add(period);

        //act
        locationRepository.save(location);
        entityManager.flush();
        entityManager.clear();

        //assert
        var reloaded = locationRepository.findById(location.getId()).orElseThrow();
        assertThat(reloaded.getWeeklyPeriods()).hasSize(1);
        assertThat(reloaded.getWeeklyPeriods().get(0).getDayOfWeek()).isEqualTo(1);
    }

    /** Saving a location cascades to its opening overrides. */
    @Test
    void savingLocationCascadesToOverrides() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-jack", "hash"));
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");
        var override = new OpeningOverrideEntity(location, LocalDate.of(2025, 12, 25),
                null, null, true, "Christmas");
        location.getOverrides().add(override);

        //act
        locationRepository.save(location);
        entityManager.flush();
        entityManager.clear();

        //assert
        var reloaded = locationRepository.findById(location.getId()).orElseThrow();
        assertThat(reloaded.getOverrides()).hasSize(1);
        assertThat(reloaded.getOverrides().get(0).getReason()).isEqualTo("Christmas");
    }

    /** Removing a period from the collection deletes it because orphanRemoval is true. */
    @Test
    void removingPeriodFromCollectionDeletesIt() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-jack", "hash"));
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");
        var period = new OpeningPeriodEntity(location, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 0);
        location.getWeeklyPeriods().add(period);
        locationRepository.save(location);
        entityManager.flush();
        entityManager.clear();

        var reloaded = locationRepository.findById(location.getId()).orElseThrow();
        assertThat(reloaded.getWeeklyPeriods()).hasSize(1);

        //act
        reloaded.getWeeklyPeriods().clear();
        locationRepository.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        //assert
        var afterRemoval = locationRepository.findById(location.getId()).orElseThrow();
        assertThat(afterRemoval.getWeeklyPeriods()).isEmpty();
    }

    /** Removing an override from the collection deletes it because orphanRemoval is true. */
    @Test
    void removingOverrideFromCollectionDeletesIt() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-jack", "hash"));
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");
        var override = new OpeningOverrideEntity(location, LocalDate.of(2025, 12, 25),
                null, null, true, "Christmas");
        location.getOverrides().add(override);
        locationRepository.save(location);
        entityManager.flush();
        entityManager.clear();

        var reloaded = locationRepository.findById(location.getId()).orElseThrow();
        assertThat(reloaded.getOverrides()).hasSize(1);

        //act
        reloaded.getOverrides().clear();
        locationRepository.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        //assert
        var afterRemoval = locationRepository.findById(location.getId()).orElseThrow();
        assertThat(afterRemoval.getOverrides()).isEmpty();
    }
}
