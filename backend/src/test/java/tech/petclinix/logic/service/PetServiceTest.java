package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.ActionEvent;
import tech.petclinix.logic.domain.Gender;
import tech.petclinix.logic.domain.Pet;
import tech.petclinix.logic.domain.PetData;
import tech.petclinix.logic.domain.Species;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.domain.exception.PetAlreadyDeactivatedException;
import tech.petclinix.logic.domain.exception.PetNameAlreadyInUseException;
import tech.petclinix.logic.domain.exception.PetPictureTooLargeException;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(repository, ownerService, eventPublisher);
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
        byte[] pictureBytes = new byte[]{1, 2, 3, 4};
        PetData petData = new PetData() {
            public String name() { return "Fluffy"; }
            public Species species() { return Species.CAT; }
            public String breed() { return "Siamese"; }
            public Gender gender() { return Gender.FEMALE; }
            public java.time.LocalDate birthDate() { return java.time.LocalDate.of(2022, 3, 10); }
            public byte[] picture() { return pictureBytes; }
            public String pictureContentType() { return "image/png"; }
        };

        when(ownerService.retrieveByUsername(username)).thenReturn(owner);
        when(repository.save(any(PetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        Pet result = petService.persist(username, petData);

        //assert
        assertThat(result.name()).isEqualTo("Fluffy");
        assertThat(result.breed()).isEqualTo("Siamese");
        assertThat(result.picture()).isEqualTo(pictureBytes);
        assertThat(result.pictureContentType()).isEqualTo("image/png");
        verify(repository).save(any(PetEntity.class));
        verify(eventPublisher).publishEvent(new ActionEvent(username, "PET_CREATED"));
    }

    /** Throws PetPictureTooLargeException when the picture exceeds the 2 MB cap, without saving. */
    @Test
    void persistThrowsPetPictureTooLargeExceptionWhenPictureExceedsCap() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        byte[] oversizedPicture = new byte[2 * 1024 * 1024 + 1];
        PetData petData = new PetData() {
            public String name() { return "Fluffy"; }
            public Species species() { return Species.CAT; }
            public String breed() { return "Siamese"; }
            public Gender gender() { return Gender.FEMALE; }
            public java.time.LocalDate birthDate() { return null; }
            public byte[] picture() { return oversizedPicture; }
            public String pictureContentType() { return "image/png"; }
        };

        when(ownerService.retrieveByUsername(username)).thenReturn(owner);

        //act + assert
        assertThatThrownBy(() -> petService.persist(username, petData))
                .isInstanceOf(PetPictureTooLargeException.class);
        verify(repository, never()).save(any(PetEntity.class));
        verifyNoInteractions(eventPublisher);
    }

    /** Throws PetNameAlreadyInUseException when the database rejects a duplicate (name, owner) pair on create. */
    @Test
    void persistThrowsPetNameAlreadyInUseExceptionOnDuplicateName() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        PetData petData = new PetData() {
            public String name() { return "Fluffy"; }
            public Species species() { return Species.CAT; }
            public String breed() { return "Siamese"; }
            public Gender gender() { return Gender.FEMALE; }
            public java.time.LocalDate birthDate() { return null; }
            public byte[] picture() { return null; }
            public String pictureContentType() { return null; }
        };

        when(ownerService.retrieveByUsername(username)).thenReturn(owner);
        when(repository.save(any(PetEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        //act + assert
        assertThatThrownBy(() -> petService.persist(username, petData))
                .isInstanceOf(PetNameAlreadyInUseException.class);
    }

    /** Applies the new field values to the existing pet entity and returns the mapped domain record. */
    @Test
    void updateAppliesNewFieldsAndReturnsDomainRecord() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var petEntity = new PetEntity("Fluffy", owner);
        PetData petData = new PetData() {
            public String name() { return "Fluffy II"; }
            public Species species() { return Species.DOG; }
            public String breed() { return "Poodle"; }
            public Gender gender() { return Gender.MALE; }
            public java.time.LocalDate birthDate() { return java.time.LocalDate.of(2021, 1, 1); }
            public byte[] picture() { return null; }
            public String pictureContentType() { return null; }
        };

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(petEntity));
        when(repository.saveAndFlush(any(PetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //act
        Pet result = petService.update(username, 1L, petData);

        //assert
        assertThat(result.name()).isEqualTo("Fluffy II");
        assertThat(result.species()).isEqualTo("DOG");
        assertThat(result.breed()).isEqualTo("Poodle");
        assertThat(result.gender()).isEqualTo("MALE");
        verify(eventPublisher).publishEvent(new ActionEvent(username, "PET_UPDATED"));
    }

    /** Throws NotFoundException when updating a pet that belongs to another owner. */
    @Test
    void updateThrowsNotFoundWhenPetBelongsToAnotherOwner() {
        //arrange
        var username = new Username("grace");
        PetData petData = new PetData() {
            public String name() { return "Fluffy II"; }
            public Species species() { return Species.DOG; }
            public String breed() { return null; }
            public Gender gender() { return null; }
            public java.time.LocalDate birthDate() { return null; }
            public byte[] picture() { return null; }
            public String pictureContentType() { return null; }
        };

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> petService.update(username, 99L, petData))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any(PetEntity.class));
    }

    /** Throws PetNameAlreadyInUseException when the database rejects a duplicate (name, owner) pair on update. */
    @Test
    void updateThrowsPetNameAlreadyInUseExceptionOnDuplicateName() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var petEntity = new PetEntity("Fluffy", owner);
        PetData petData = new PetData() {
            public String name() { return "Spot"; }
            public Species species() { return null; }
            public String breed() { return null; }
            public Gender gender() { return null; }
            public java.time.LocalDate birthDate() { return null; }
            public byte[] picture() { return null; }
            public String pictureContentType() { return null; }
        };

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(petEntity));
        when(repository.saveAndFlush(any(PetEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        //act + assert
        assertThatThrownBy(() -> petService.update(username, 1L, petData))
                .isInstanceOf(PetNameAlreadyInUseException.class);
    }

    /** Deactivates an active pet and publishes a PET_DEACTIVATED event. */
    @Test
    void deactivateSetsInactiveAndPublishesEvent() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var petEntity = new PetEntity("Fluffy", owner);

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(petEntity));
        when(repository.save(any(PetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //act
        petService.deactivate(username, 1L);

        //assert
        assertThat(petEntity.isActive()).isFalse();
        verify(repository).save(petEntity);
        verify(eventPublisher).publishEvent(new ActionEvent(username, "PET_DEACTIVATED"));
    }

    /** Throws PetAlreadyDeactivatedException when the pet is already inactive. */
    @Test
    void deactivateThrowsPetAlreadyDeactivatedExceptionWhenAlreadyInactive() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var petEntity = new PetEntity("Fluffy", owner);
        petEntity.deactivate();

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(petEntity));

        //act + assert
        assertThatThrownBy(() -> petService.deactivate(username, 1L))
                .isInstanceOf(PetAlreadyDeactivatedException.class);
        verify(repository, never()).save(any(PetEntity.class));
        verifyNoInteractions(eventPublisher);
    }

    /** Excludes deactivated pets from the owner's pet list by AND-ing the active specification. */
    @Test
    void findAllByOwnerExcludesInactivePets() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var activePet = new PetEntity("Fluffy", owner);

        when(ownerService.retrieveByUsername(username)).thenReturn(owner);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(activePet));

        //act
        List<Pet> result = petService.findAllByOwner(username);

        //assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Fluffy");
        verify(repository).findAll(any(Specification.class));
    }
}
