package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.Gender;
import tech.petclinix.logic.domain.Pet;
import tech.petclinix.logic.domain.PetData;
import tech.petclinix.logic.domain.Species;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.jpa.PetJpaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link PetService}.
 *
 * Repositories and collaborating services are mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetJpaRepository repository;

    @Mock
    private OwnerService ownerService;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(repository, ownerService);
    }

    /** Returns all pets belonging to the given owner. */
    @Test
    void findAllByOwnerReturnsMappedPets() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var petEntity = new PetEntity("Fluffy", owner);

        when(ownerService.retrieveByUsername(username)).thenReturn(owner);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(petEntity));

        //act
        List<Pet> result = petService.findAllByOwner(username);

        //assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Fluffy");
        verify(ownerService).retrieveByUsername(username);
        verify(repository).findAll(any(Specification.class));
    }

    /** Returns an empty list when the owner has no pets. */
    @Test
    void findAllByOwnerReturnsEmptyListWhenOwnerHasNoPets() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");

        when(ownerService.retrieveByUsername(username)).thenReturn(owner);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        //act
        List<Pet> result = petService.findAllByOwner(username);

        //assert
        assertThat(result).isEmpty();
    }

    /** Throws NotFoundException when no pet with the given id belongs to the owner. */
    @Test
    void retrieveByOwnerAndIdThrowsNotFoundWhenPetDoesNotExist() {
        //arrange
        var username = new Username("grace");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> petService.retrieveByOwnerAndId(username, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    /** Returns the pet entity when found by owner and id. */
    @Test
    void retrieveByOwnerAndIdReturnsPetEntityWhenFound() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var petEntity = new PetEntity("Fluffy", owner);

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(petEntity));

        //act
        var result = petService.retrieveByOwnerAndId(username, 1L);

        //assert
        assertThat(result.getName()).isEqualTo("Fluffy");
    }

    /** Saves a new pet entity with all fields and returns the mapped domain record. */
    @Test
    void persistSavesPetAndReturnsDomainRecord() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        PetData petData = new PetData() {
            public String name() { return "Fluffy"; }
            public Species species() { return Species.CAT; }
            public Gender gender() { return Gender.FEMALE; }
            public java.time.LocalDate birthDate() { return java.time.LocalDate.of(2022, 3, 10); }
        };

        when(ownerService.retrieveByUsername(username)).thenReturn(owner);
        when(repository.save(any(PetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        Pet result = petService.persist(username, petData);

        //assert
        assertThat(result.name()).isEqualTo("Fluffy");
        verify(repository).save(any(PetEntity.class));
    }
}
