package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.jpa.VisitJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link VisitService}.
 *
 * Repository is mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock
    private VisitJpaRepository repository;

    private VisitService visitService;

    @BeforeEach
    void setUp() {
        visitService = new VisitService(repository);
    }

    private AppointmentEntity buildAppointment() {
        var owner = new OwnerEntity("grace", "hash");
        var vet = new VetEntity("vet-jack", "hash");
        var pet = new PetEntity("Fluffy", owner);
        return new AppointmentEntity(vet, pet, LocalDateTime.of(2025, 6, 1, 10, 0));
    }

    /** Returns the visit entity when found by appointment. */
    @Test
    void retrieveByAppointmentReturnsVisitEntityWhenFound() {
        //arrange
        var appointment = buildAppointment();
        var visitEntity = new VisitEntity(appointment);
        visitEntity.setVetSummary("All good");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(visitEntity));

        //act
        VisitEntity result = visitService.retrieveByAppointment(appointment);

        //assert
        assertThat(result.getVetSummary()).isEqualTo("All good");
    }

    /** Throws NotFoundException when no visit is found for the given appointment. */
    @Test
    void retrieveByAppointmentThrowsNotFoundWhenVisitDoesNotExist() {
        //arrange
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> visitService.retrieveByAppointment(appointment))
                .isInstanceOf(NotFoundException.class);
    }

    /** Returns all visits belonging to the given pet. */
    @Test
    void findAllByPetReturnsList() {
        //arrange
        var owner = new OwnerEntity("grace", "hash");
        var pet = new PetEntity("Fluffy", owner);
        var appointment = buildAppointment();
        var visitEntity = new VisitEntity(appointment);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(visitEntity));

        //act
        var result = visitService.findAllByPet(pet);

        //assert
        assertThat(result).hasSize(1);
        verify(repository).findAll(any(Specification.class));
    }

    /** Creates a new visit when none exists for the appointment and saves it. */
    @Test
    void persistCreatesNewVisitWhenNoneExistsForAppointment() {
        //arrange
        var appointment = buildAppointment();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(repository.save(any(VisitEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        VisitEntity result = visitService.persist(appointment, "vet summary", "owner summary", "vaccine");

        //assert
        assertThat(result.getVetSummary()).isEqualTo("vet summary");
        assertThat(result.getOwnerSummary()).isEqualTo("owner summary");
        assertThat(result.getVaccination()).isEqualTo("vaccine");
        verify(repository).save(any(VisitEntity.class));
    }

    /** Updates an existing visit when one already exists for the appointment. */
    @Test
    void persistUpdatesExistingVisitWhenOneAlreadyExists() {
        //arrange
        var appointment = buildAppointment();
        var existingVisit = new VisitEntity(appointment);
        existingVisit.setVetSummary("old summary");

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(existingVisit));
        when(repository.save(any(VisitEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        VisitEntity result = visitService.persist(appointment, "new vet summary", "new owner summary", "new vaccine");

        //assert
        assertThat(result.getVetSummary()).isEqualTo("new vet summary");
        assertThat(result.getOwnerSummary()).isEqualTo("new owner summary");
    }
}
