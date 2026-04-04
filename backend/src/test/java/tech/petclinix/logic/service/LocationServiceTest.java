package tech.petclinix.logic.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.Location;
import tech.petclinix.logic.domain.LocationData;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.LocationJpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link LocationService}.
 *
 * Repository, EntityManager, and collaborating VetService are mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationJpaRepository repository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private VetService vetService;

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(repository, entityManager, vetService);
    }

    /** Returns all locations belonging to the given vet. */
    @Test
    void findAllByVetReturnsMappedLocations() {
        //arrange
        var username = new Username("vet-jack");
        var vet = new VetEntity("vet-jack", "hash");
        var locationEntity = new LocationEntity(vet, "Clinic North", "Europe/Vienna");

        when(vetService.retrieveByUsername(username)).thenReturn(vet);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(locationEntity));

        //act
        List<Location> result = locationService.findAllByVet(username);

        //assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Clinic North");
        verify(vetService).retrieveByUsername(username);
    }

    /** Returns an empty list when the vet has no locations. */
    @Test
    void findAllByVetReturnsEmptyListWhenVetHasNoLocations() {
        //arrange
        var username = new Username("vet-jack");
        var vet = new VetEntity("vet-jack", "hash");

        when(vetService.retrieveByUsername(username)).thenReturn(vet);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        //act
        List<Location> result = locationService.findAllByVet(username);

        //assert
        assertThat(result).isEmpty();
    }

    /** Throws NotFoundException when no location with the given id exists. */
    @Test
    void findByVetAndIdThrowsNotFoundWhenLocationDoesNotExist() {
        //arrange
        var username = new Username("vet-jack");
        var vet = new VetEntity("vet-jack", "hash");

        when(vetService.retrieveByUsername(username)).thenReturn(vet);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> locationService.findByVetAndId(username, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    /** Saves a new location entity and returns the mapped domain record. */
    @Test
    void persistSavesLocationAndReturnsDomainRecord() {
        //arrange
        var username = new Username("vet-jack");
        var vet = new VetEntity("vet-jack", "hash");
        LocationData locationData = new Location(null, "Clinic North", "Europe/Vienna",
                List.of(new Location.OpeningPeriodResponse(1, LocalTime.of(9, 0), LocalTime.of(17, 0), 0)),
                List.of());

        when(vetService.retrieveByUsername(username)).thenReturn(vet);
        when(repository.save(any(LocationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        Location result = locationService.persist(username, locationData);

        //assert
        assertThat(result.name()).isEqualTo("Clinic North");
        assertThat(result.zoneId()).isEqualTo("Europe/Vienna");
        verify(repository).save(any(LocationEntity.class));
    }

    /** Updates location fields and replaces child collections, then returns the updated domain record. */
    @Test
    void updateReplacesPeriodsAndReturnsDomainRecord() throws Exception {
        //arrange
        var username = new Username("vet-jack");
        var vet = new VetEntity("vet-jack", "hash");
        // Set the vet id via reflection so the ownership check (location.getVet().getId().equals(vet.getId())) passes
        var idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(vet, 42L);

        var locationEntity = new LocationEntity(vet, "Old Name", "Europe/Vienna");

        LocationData updateData = new Location(null, "New Name", "Europe/London",
                List.of(new Location.OpeningPeriodResponse(2, LocalTime.of(8, 0), LocalTime.of(16, 0), 0)),
                List.of());

        when(vetService.retrieveByUsername(username)).thenReturn(vet);
        when(repository.findById(1L)).thenReturn(Optional.of(locationEntity));
        when(repository.save(any(LocationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        Location result = locationService.update(username, 1L, updateData);

        //assert
        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.zoneId()).isEqualTo("Europe/London");
        verify(entityManager).flush();
        verify(repository).save(any(LocationEntity.class));
    }
}
