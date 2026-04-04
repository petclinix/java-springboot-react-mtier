package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.VetEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link LocationJpaRepository}.
 *
 * Verifies the {@code Specifications.byVet} and {@code Specifications.byId} queries execute
 * correctly against H2. Happy path only — no mocking, full JPA stack loaded via {@code @DataJpaTest}.
 */
@DataJpaTest
class LocationJpaRepositoryIntegrationTest {

    @Autowired
    LocationJpaRepository locationRepository;

    @Autowired
    VetJpaRepository vetRepository;

    /** Returns all locations belonging to the given vet entity. */
    @Test
    void byVetFindsAllLocationsForThatVet() {
        //arrange
        var vetJack = vetRepository.save(new VetEntity("vet-jack", "hash1"));
        var vetKate = vetRepository.save(new VetEntity("vet-kate", "hash2"));
        locationRepository.save(new LocationEntity(vetJack, "Clinic North", "Europe/Vienna"));
        locationRepository.save(new LocationEntity(vetJack, "Clinic South", "Europe/Vienna"));
        locationRepository.save(new LocationEntity(vetKate, "Clinic East", "Europe/Vienna"));

        //act
        var results = locationRepository.findAll(
                LocationJpaRepository.Specifications.byVet(vetJack));

        //assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting(LocationEntity::getName)
                .containsExactlyInAnyOrder("Clinic North", "Clinic South");
    }

    /** Returns the location matching the combined id and vet predicate. */
    @Test
    void byIdAndByVetFindOneReturnsMatchingLocation() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-jack", "hash1"));
        var otherVet = vetRepository.save(new VetEntity("vet-kate", "hash2"));
        var location = locationRepository.save(new LocationEntity(vet, "Clinic North", "Europe/Vienna"));
        locationRepository.save(new LocationEntity(otherVet, "Clinic East", "Europe/Vienna"));

        //act
        var result = locationRepository.findOne(
                LocationJpaRepository.Specifications.byId(location.getId())
                        .and(LocationJpaRepository.Specifications.byVet(vet)));

        //assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Clinic North");
    }

    /** Returns empty when the location id exists but belongs to a different vet. */
    @Test
    void byIdAndByVetFindOneReturnsEmptyWhenVetDoesNotMatch() {
        //arrange
        var vet = vetRepository.save(new VetEntity("vet-jack", "hash1"));
        var otherVet = vetRepository.save(new VetEntity("vet-kate", "hash2"));
        var location = locationRepository.save(new LocationEntity(otherVet, "Clinic East", "Europe/Vienna"));

        //act
        var result = locationRepository.findOne(
                LocationJpaRepository.Specifications.byId(location.getId())
                        .and(LocationJpaRepository.Specifications.byVet(vet)));

        //assert
        assertThat(result).isEmpty();
    }

    /** Returns an empty list when the vet has no locations. */
    @Test
    void byVetReturnsEmptyListWhenVetHasNoLocations() {
        //arrange
        var vetLeo = vetRepository.save(new VetEntity("vet-leo", "hash1"));

        //act
        var results = locationRepository.findAll(
                LocationJpaRepository.Specifications.byVet(vetLeo));

        //assert
        assertThat(results).isEmpty();
    }
}
