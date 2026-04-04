package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.VetEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link VetJpaRepository}.
 *
 * Verifies the {@code Specifications.byUsername} query executes correctly against H2.
 * Happy path only — no mocking, full JPA stack loaded via {@code @DataJpaTest}.
 */
@DataJpaTest
class VetJpaRepositoryIntegrationTest {

    @Autowired
    VetJpaRepository vetRepository;

    /** Returns the VetEntity whose username matches the given value. */
    @Test
    void byUsernameFindsMatchingVet() {
        //arrange
        vetRepository.save(new VetEntity("vet-elena", "hash1"));
        vetRepository.save(new VetEntity("vet-frank", "hash2"));

        //act
        var results = vetRepository.findAll(
                VetJpaRepository.Specifications.byUsername(new Username("vet-elena")));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("vet-elena");
    }

    /** Returns an empty list when no vet matches the given username. */
    @Test
    void byUsernameReturnsEmptyListWhenNoMatch() {
        //arrange
        vetRepository.save(new VetEntity("vet-elena", "hash1"));

        //act
        var results = vetRepository.findAll(
                VetJpaRepository.Specifications.byUsername(new Username("unknown")));

        //assert
        assertThat(results).isEmpty();
    }
}
