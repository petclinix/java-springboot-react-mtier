package tech.petclinix.logic.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tech.petclinix.logic.domain.Gender;
import tech.petclinix.logic.domain.PetData;
import tech.petclinix.logic.domain.Species;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.PetNameAlreadyInUseException;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link PetService}.
 *
 * Uses a real, full Spring context (not {@code @DataJpaTest}, whose per-test transaction
 * rollback and different flush behaviour would not reproduce Hibernate's real deferred-flush
 * timing on {@code save()} of an already-managed entity). Verifies that {@link PetService#update}
 * translates a real database unique-constraint violation into {@link PetNameAlreadyInUseException}
 * rather than letting a raw persistence exception escape at transaction commit.
 */
@SpringBootTest
class PetServiceIntegrationTest {

    @Autowired
    PetService petService;

    @Autowired
    OwnerJpaRepository ownerRepository;

    @Autowired
    PetJpaRepository petRepository;

    /** Renaming a pet to another of the same owner's existing pet names throws PetNameAlreadyInUseException, not a raw persistence exception. */
    @Test
    void updateThrowsPetNameAlreadyInUseExceptionOnRealDuplicateNameConstraintViolation() {
        //arrange
        var suffix = "-" + System.nanoTime();
        var owner = ownerRepository.save(new OwnerEntity("pet-update-owner" + suffix, "hash"));
        petRepository.save(new PetEntity("Fluffy", owner));
        PetEntity second = petRepository.save(new PetEntity("Rex", owner));

        PetData petData = new PetData() {
            public String name() { return "Fluffy"; }
            public Species species() { return Species.DOG; }
            public String breed() { return null; }
            public Gender gender() { return null; }
            public java.time.LocalDate birthDate() { return null; }
            public byte[] picture() { return null; }
            public String pictureContentType() { return null; }
        };

        //act + assert
        assertThatThrownBy(() -> petService.update(new Username(owner.getUsername()), second.getId(), petData))
                .isInstanceOf(PetNameAlreadyInUseException.class);
    }
}
