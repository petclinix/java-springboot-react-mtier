package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.VetEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link UserJpaRepository}.
 *
 * Verifies the {@code Specifications.byUsername} query executes correctly against H2.
 * Happy path only — no mocking, full JPA stack loaded via {@code @DataJpaTest}.
 */
@DataJpaTest
class UserJpaRepositoryIntegrationTest {

    @Autowired
    UserJpaRepository userRepository;

    @Autowired
    OwnerJpaRepository ownerRepository;

    @Autowired
    VetJpaRepository vetRepository;

    /** Returns the single UserEntity whose username matches the given value. */
    @Test
    void byUsernameFindsMatchingUser() {
        //arrange
        ownerRepository.save(new OwnerEntity("owner-alice", "hash1"));
        vetRepository.save(new VetEntity("vet-bob", "hash2"));

        //act
        var results = userRepository.findAll(
                UserJpaRepository.Specifications.byUsername(new Username("owner-alice")));

        //assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("owner-alice");
    }

    /** Returns an empty list when no user matches the given username. */
    @Test
    void byUsernameReturnsEmptyListWhenNoMatch() {
        //arrange
        ownerRepository.save(new OwnerEntity("owner-alice", "hash1"));

        //act
        var results = userRepository.findAll(
                UserJpaRepository.Specifications.byUsername(new Username("unknown")));

        //assert
        assertThat(results).isEmpty();
    }
}
