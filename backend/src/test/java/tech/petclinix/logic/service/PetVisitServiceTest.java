package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.OwnerVisit;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VisitEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link PetVisitService}.
 *
 * PetService and VisitService are mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class PetVisitServiceTest {

    @Mock
    private PetService petService;

    @Mock
    private VisitService visitService;

    private PetVisitService petVisitService;

    @BeforeEach
    void setUp() {
        petVisitService = new PetVisitService(petService, visitService);
    }

    /** Returns all visits for the given pet belonging to the owner, mapped to domain records. */
    @Test
    void findAllVisitsByOwnerAndPetReturnsMappedOwnerVisits() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var appointment = new AppointmentEntity(vet, pet, LocalDateTime.of(2025, 6, 1, 10, 0));
        var visitEntity = new VisitEntity(appointment);
        visitEntity.setOwnerSummary("Good visit");

        when(petService.retrieveByOwnerAndId(username, 2L)).thenReturn(pet);
        when(visitService.findAllByPet(pet)).thenReturn(List.of(visitEntity));

        //act
        List<OwnerVisit> result = petVisitService.findAllVisitsByOwnerAndPet(username, 2L);

        //assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).ownerSummary()).isEqualTo("Good visit");
        assertThat(result.get(0).vetUsername()).isEqualTo("vet-jack");
        verify(petService).retrieveByOwnerAndId(username, 2L);
        verify(visitService).findAllByPet(pet);
    }

    /** Returns an empty list when the pet has no visits. */
    @Test
    void findAllVisitsByOwnerAndPetReturnsEmptyListWhenNoVisits() {
        //arrange
        var username = new Username("grace");
        var owner = new OwnerEntity("grace", "hash");
        var pet = new PetEntity("Fluffy", owner);

        when(petService.retrieveByOwnerAndId(username, 2L)).thenReturn(pet);
        when(visitService.findAllByPet(pet)).thenReturn(List.of());

        //act
        List<OwnerVisit> result = petVisitService.findAllVisitsByOwnerAndPet(username, 2L);

        //assert
        assertThat(result).isEmpty();
    }
}
