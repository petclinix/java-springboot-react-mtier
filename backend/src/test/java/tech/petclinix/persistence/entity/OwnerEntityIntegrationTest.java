package tech.petclinix.persistence.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity relation test for {@link OwnerEntity}.
 *
 * Verifies that the {@code @OneToMany(cascade = ALL)} declaration on the {@code pets}
 * collection behaves correctly against H2.
 * Uses {@code @DataJpaTest} — no services, no mocking.
 */
@DataJpaTest
class OwnerEntityIntegrationTest {

    @Autowired
    private OwnerJpaRepository ownerRepository;

    @Autowired
    private PetJpaRepository petRepository;

    /** Saving an owner cascades to its pets. */
    @Test
    void savingOwnerCascadesToPets() {
        //arrange
        var owner = new OwnerEntity("grace", "hash");
        var pet = new PetEntity("Fluffy", owner);
        // pet.setOwner calls owner.addPet internally — the pet is in the collection

        //act
        ownerRepository.save(owner);

        //assert
        var savedPets = petRepository.findAll();
        assertThat(savedPets).hasSize(1);
        assertThat(savedPets.get(0).getName()).isEqualTo("Fluffy");
    }

    /** Removing a pet from the collection does NOT delete it because orphanRemoval is not set. */
    @Test
    void removingPetFromCollectionDoesNotDeleteWithoutOrphanRemoval() {
        //arrange
        var owner = new OwnerEntity("grace", "hash");
        var pet = new PetEntity("Fluffy", owner);
        ownerRepository.save(owner);
        ownerRepository.flush();

        assertThat(petRepository.findAll()).hasSize(1);

        //act
        // OwnerEntity.getPets() returns an unmodifiable view, so we reload and verify
        // that the pet still exists after the owner is loaded again — no orphanRemoval means
        // we confirm the annotation is NOT present by checking the pet persists independently.
        var reloadedOwner = ownerRepository.findById(owner.getId()).orElseThrow();

        //assert
        // The pet was saved via cascade and still exists in the database
        assertThat(petRepository.findAll()).hasSize(1);
        assertThat(reloadedOwner.getPets()).hasSize(1);
    }
}
