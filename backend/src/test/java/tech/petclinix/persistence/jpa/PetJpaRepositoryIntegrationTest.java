package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link PetJpaRepository}.
 *
 * Verifies each Specification — {@code byOwner}, {@code byOwnerUsername}, and {@code byId}
 * — executes correctly against H2.
 * Happy path only — no mocking, full JPA stack loaded via {@code @DataJpaTest}.
 */
@DataJpaTest
class PetJpaRepositoryIntegrationTest {

    @Autowired
    PetJpaRepository petRepository;

    @Autowired
    OwnerJpaRepository ownerRepository;

    private OwnerEntity ownerGrace;
    private OwnerEntity ownerHenry;
    private PetEntity petFluffy;
    private PetEntity petSpot;

    @BeforeEach
    void setUp() {
        ownerGrace = ownerRepository.save(new OwnerEntity("owner-grace", "hash1"));
        ownerHenry = ownerRepository.save(new OwnerEntity("owner-henry", "hash2"));
        petFluffy = petRepository.save(new PetEntity("Fluffy", ownerGrace));
        petSpot = petRepository.save(new PetEntity("Spot", ownerHenry));
    }

    /** Returns all pets belonging to the given owner entity. */
    @Test
    void byOwnerFindsAllPetsForThatOwner() {
        //act
        var results = petRepository.findAll(
                PetJpaRepository.Specifications.byOwner(ownerGrace));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Fluffy");
    }

    /** Returns all pets whose owner has the given username. */
    @Test
    void byOwnerUsernameFindsAllPetsForThatUsername() {
        //act
        var results = petRepository.findAll(
                PetJpaRepository.Specifications.byOwnerUsername(new Username("owner-henry")));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Spot");
    }

    /** Returns the pet matching the given id. */
    @Test
    void byIdFindsMatchingPet() {
        //act
        var results = petRepository.findAll(
                PetJpaRepository.Specifications.byId(petFluffy.getId()));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Fluffy");
    }

    /** Returns an empty list when no pet belongs to the given owner. */
    @Test
    void byOwnerReturnsEmptyListWhenOwnerHasNoPets() {
        //arrange
        var ownerIvy = ownerRepository.save(new OwnerEntity("owner-ivy", "hash3"));

        //act
        var results = petRepository.findAll(
                PetJpaRepository.Specifications.byOwner(ownerIvy));

        //assert
        assertThat(results).isEmpty();
    }
}
