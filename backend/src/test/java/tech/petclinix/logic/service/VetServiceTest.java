package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.Vet;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.VetJpaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link VetService}.
 *
 * Repository is mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class VetServiceTest {

    @Mock
    private VetJpaRepository repository;

    private VetService vetService;

    @BeforeEach
    void setUp() {
        vetService = new VetService(repository);
    }

    /** Returns all vets mapped to domain records. */
    @Test
    void findAllReturnsMappedVets() {
        //arrange
        var vetEntity = new VetEntity("vet-jack", "hash");
        when(repository.findAll()).thenReturn(List.of(vetEntity));

        //act
        List<Vet> result = vetService.findAll();

        //assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("vet-jack");
        verify(repository).findAll();
    }

    /** Returns an empty list when no vets are registered. */
    @Test
    void findAllReturnsEmptyListWhenNoVets() {
        //arrange
        when(repository.findAll()).thenReturn(List.of());

        //act
        List<Vet> result = vetService.findAll();

        //assert
        assertThat(result).isEmpty();
    }

    /** Returns the vet entity when found by id. */
    @Test
    void retrieveByIdReturnsVetEntityWhenFound() {
        //arrange
        var vetEntity = new VetEntity("vet-jack", "hash");
        when(repository.findById(1L)).thenReturn(Optional.of(vetEntity));

        //act
        VetEntity result = vetService.retrieveById(1L);

        //assert
        assertThat(result.getUsername()).isEqualTo("vet-jack");
    }

    /** Throws NotFoundException when no vet with the given id exists. */
    @Test
    void retrieveByIdThrowsNotFoundWhenVetDoesNotExist() {
        //arrange
        when(repository.findById(99L)).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> vetService.retrieveById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    /** Returns the vet entity when found by username. */
    @Test
    void retrieveByUsernameReturnsVetEntityWhenFound() {
        //arrange
        var username = new Username("vet-jack");
        var vetEntity = new VetEntity("vet-jack", "hash");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(vetEntity));

        //act
        VetEntity result = vetService.retrieveByUsername(username);

        //assert
        assertThat(result.getUsername()).isEqualTo("vet-jack");
    }

    /** Throws NotFoundException when no vet with the given username exists. */
    @Test
    void retrieveByUsernameThrowsNotFoundWhenVetDoesNotExist() {
        //arrange
        var username = new Username("unknown");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> vetService.retrieveByUsername(username))
                .isInstanceOf(NotFoundException.class);
    }

    /** Returns an empty Optional when no vet with the given username exists. */
    @Test
    void findByUsernameReturnsEmptyOptionalWhenVetDoesNotExist() {
        //arrange
        var username = new Username("unknown");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act
        var result = vetService.findByUsername(username);

        //assert
        assertThat(result).isEmpty();
    }

    /** Returns an Optional containing the vet entity when found. */
    @Test
    void findByUsernameReturnsOptionalWithVetWhenFound() {
        //arrange
        var username = new Username("vet-jack");
        var vetEntity = new VetEntity("vet-jack", "hash");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(vetEntity));

        //act
        var result = vetService.findByUsername(username);

        //assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("vet-jack");
    }
}
