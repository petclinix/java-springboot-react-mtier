package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.OwnerEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link OwnerJpaRepository}.
 *
 * Verifies the {@code Specifications.byUsername} query executes correctly against H2.
 * Happy path only — no mocking, full JPA stack loaded via {@code @DataJpaTest}.
 */
@DataJpaTest
class OwnerJpaRepositoryIntegrationTest {

    @Autowired
    OwnerJpaRepository ownerRepository;

    /** Returns the OwnerEntity whose username matches the given value. */
    @Test
    void byUsernameFindsMatchingOwner() {
        //arrange
        ownerRepository.save(new OwnerEntity("owner-carol", "hash1"));
        ownerRepository.save(new OwnerEntity("owner-dave", "hash2"));

        //act
        var results = ownerRepository.findAll(
                OwnerJpaRepository.Specifications.byUsername(new Username("owner-carol")));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("owner-carol");
    }

    /** Returns an empty list when no owner matches the given username. */
    @Test
    void byUsernameReturnsEmptyListWhenNoMatch() {
        //arrange
        ownerRepository.save(new OwnerEntity("owner-carol", "hash1"));

        //act
        var results = ownerRepository.findAll(
                OwnerJpaRepository.Specifications.byUsername(new Username("unknown")));

        //assert
        assertThat(results).isEmpty();
    }
}
