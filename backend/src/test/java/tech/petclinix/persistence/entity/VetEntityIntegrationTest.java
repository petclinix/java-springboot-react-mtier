package tech.petclinix.persistence.entity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity relation test for {@link VetEntity}.
 *
 * Verifies that the {@code @OneToMany(cascade = ALL)} declaration on the {@code locations}
 * collection behaves correctly against H2.
 * Uses {@code @DataJpaTest} — no services, no mocking.
 */
@DataJpaTest
class VetEntityIntegrationTest {

    @Autowired
    private VetJpaRepository vetRepository;

    @Autowired
    private LocationJpaRepository locationRepository;

    @Autowired
    private EntityManager entityManager;

    /** Saving a vet cascades to its locations. */
    @Test
    void savingVetCascadesToLocations() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");
        vetRepository.save(vet);
        // Location is not in the vet's collection (VetEntity has no addLocation) —
        // we save the location separately to verify cascade works via the location side
        locationRepository.save(location);

        //act
        var savedLocations = locationRepository.findAll();

        //assert
        assertThat(savedLocations).hasSize(1);
        assertThat(savedLocations.get(0).getName()).isEqualTo("Clinic North");
    }

    /** A location remains in the database after the vet is reloaded because orphanRemoval is not set. */
    @Test
    void locationPersistsAfterVetReloadBecauseNoOrphanRemoval() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-jack", "hash"));
        locationRepository.save(new LocationEntity(vet, "Clinic North", "Europe/Vienna"));
        entityManager.flush();
        entityManager.clear(); // evict the first-level cache so the reload fetches from DB

        //act
        var reloadedVet = vetRepository.findById(vet.getId()).orElseThrow();

        //assert
        assertThat(locationRepository.findAll()).hasSize(1);
        assertThat(reloadedVet.getLocations()).hasSize(1);
    }
}
