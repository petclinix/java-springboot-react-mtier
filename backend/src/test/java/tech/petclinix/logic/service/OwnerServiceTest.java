package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link OwnerService}.
 *
 * Repository is mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private OwnerJpaRepository repository;

    private OwnerService ownerService;

    @BeforeEach
    void setUp() {
        ownerService = new OwnerService(repository);
    }

    /** Returns the owner entity when found by username. */
    @Test
    void retrieveByUsernameReturnsOwnerEntityWhenFound() {
        //arrange
        var username = new Username("grace");
        var ownerEntity = new OwnerEntity("grace", "hash");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(ownerEntity));

        //act
        OwnerEntity result = ownerService.retrieveByUsername(username);

        //assert
        assertThat(result.getUsername()).isEqualTo("grace");
        verify(repository).findOne(any(Specification.class));
    }

    /** Throws NotFoundException when no owner with the given username exists. */
    @Test
    void retrieveByUsernameThrowsNotFoundWhenOwnerDoesNotExist() {
        //arrange
        var username = new Username("unknown");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> ownerService.retrieveByUsername(username))
                .isInstanceOf(NotFoundException.class);
    }

    /** Returns an Optional containing the owner entity when found. */
    @Test
    void findByUsernameReturnsOptionalWithOwnerWhenFound() {
        //arrange
        var username = new Username("grace");
        var ownerEntity = new OwnerEntity("grace", "hash");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(ownerEntity));

        //act
        var result = ownerService.findByUsername(username);

        //assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("grace");
    }

    /** Returns an empty Optional when no owner with the given username exists. */
    @Test
    void findByUsernameReturnsEmptyOptionalWhenOwnerDoesNotExist() {
        //arrange
        var username = new Username("unknown");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act
        var result = ownerService.findByUsername(username);

        //assert
        assertThat(result).isEmpty();
    }
}
