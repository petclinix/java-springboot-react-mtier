package tech.petclinix.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link VetEntity}.
 *
 * Verifies that the {@code @OneToMany locations} back-pointer is set correctly
 * in Java when a location is constructed with a vet. No database involved.
 */
class VetEntityTest {

    /** Constructing a location with a vet sets the vet back-pointer on the location. */
    @Test
    void locationConstructorSetsVetBackPointer() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");

        //act
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");

        //assert
        assertThat(location.getVet()).isSameAs(vet);
    }
}
