package tech.petclinix.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link OwnerEntity}.
 *
 * Verifies that the {@code @OneToMany pets} collection maintains the bidirectional
 * back-pointer consistently in Java. No database involved.
 */
class OwnerEntityTest {

    /** Adding a pet via its constructor sets the back-pointer on both sides. */
    @Test
    void newPetWithOwnerSetsBackPointerOnBothSides() {
        //arrange
        var owner = new OwnerEntity("grace", "hash");

        //act
        var pet = new PetEntity("Fluffy", owner);

        //assert
        assertThat(pet.getOwner()).isSameAs(owner);
        assertThat(owner.getPets()).contains(pet);
    }
}
