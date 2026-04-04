package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.StatsData;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link StatsService}.
 *
 * All repositories are mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private OwnerJpaRepository ownerJpaRepository;

    @Mock
    private VetJpaRepository vetJpaRepository;

    @Mock
    private PetJpaRepository petJpaRepository;

    @Mock
    private AppointmentJpaRepository appointmentJpaRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(ownerJpaRepository, vetJpaRepository, petJpaRepository, appointmentJpaRepository);
    }

    /** Returns aggregated counts from all four repositories. */
    @Test
    void getStatsReturnsAggregatedCounts() {
        //arrange
        when(ownerJpaRepository.count()).thenReturn(5L);
        when(vetJpaRepository.count()).thenReturn(3L);
        when(petJpaRepository.count()).thenReturn(12L);
        when(appointmentJpaRepository.count()).thenReturn(20L);
        when(appointmentJpaRepository.countPerVet()).thenReturn(List.of(
                new StatsData.VetAppointmentCount("vet-jack", 10L),
                new StatsData.VetAppointmentCount("vet-kate", 10L)
        ));

        //act
        StatsData result = statsService.getStats();

        //assert
        assertThat(result.totalOwners()).isEqualTo(5L);
        assertThat(result.totalVets()).isEqualTo(3L);
        assertThat(result.totalPets()).isEqualTo(12L);
        assertThat(result.totalAppointments()).isEqualTo(20L);
        assertThat(result.appointmentsPerVet()).hasSize(2);
        verify(ownerJpaRepository).count();
        verify(vetJpaRepository).count();
        verify(petJpaRepository).count();
        verify(appointmentJpaRepository).count();
        verify(appointmentJpaRepository).countPerVet();
    }

    /** Returns zero counts and an empty per-vet list when the system has no data. */
    @Test
    void getStatsReturnsZeroCountsWhenSystemIsEmpty() {
        //arrange
        when(ownerJpaRepository.count()).thenReturn(0L);
        when(vetJpaRepository.count()).thenReturn(0L);
        when(petJpaRepository.count()).thenReturn(0L);
        when(appointmentJpaRepository.count()).thenReturn(0L);
        when(appointmentJpaRepository.countPerVet()).thenReturn(List.of());

        //act
        StatsData result = statsService.getStats();

        //assert
        assertThat(result.totalOwners()).isZero();
        assertThat(result.totalVets()).isZero();
        assertThat(result.totalPets()).isZero();
        assertThat(result.totalAppointments()).isZero();
        assertThat(result.appointmentsPerVet()).isEmpty();
    }
}
