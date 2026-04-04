package tech.petclinix.logic.service;

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
import tech.petclinix.persistence.entity.OpeningPeriodEntity;
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
 * Repository and collaborating VetService are mocked — no database, no EntityManager.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationJpaRepository repository;

    @Mock
    private VetService vetService;

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(repository, vetService);
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

    /** Deletes the location entity when it belongs to the requesting vet. */
    @Test
    void deleteRemovesLocationWhenOwnedByVet() throws Exception {
        //arrange
        var username = new Username("vet-jack");
        var vet = new VetEntity("vet-jack", "hash");
        var idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(vet, 42L);
        var locationEntity = new LocationEntity(vet, "Clinic North", "Europe/Vienna");

        when(vetService.retrieveByUsername(username)).thenReturn(vet);
        when(repository.findById(1L)).thenReturn(Optional.of(locationEntity));

        //act
        locationService.delete(username, 1L);

        //assert
        verify(repository).delete(locationEntity);
    }

    /** Throws NotFoundException when the location does not belong to the requesting vet. */
    @Test
    void deleteThrowsNotFoundWhenLocationBelongsToAnotherVet() throws Exception {
        //arrange
        var username = new Username("vet-jack");
        var vet = new VetEntity("vet-jack", "hash");
        var idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(vet, 42L);

        var otherVet = new VetEntity("other-vet", "hash");
        var otherIdField = UserEntity.class.getDeclaredField("id");
        otherIdField.setAccessible(true);
        otherIdField.set(otherVet, 99L);

        var locationEntity = new LocationEntity(otherVet, "Other Clinic", "Europe/Vienna");

        when(vetService.retrieveByUsername(username)).thenReturn(vet);
        when(repository.findById(1L)).thenReturn(Optional.of(locationEntity));

        //act + assert
        assertThatThrownBy(() -> locationService.delete(username, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    /** Saves a new location with periods synced and returns the mapped domain record. */
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
        assertThat(result.weeklyPeriods()).hasSize(1);
        verify(repository).save(any(LocationEntity.class));
    }

    /** Updates location fields and syncs collections in place, then returns the updated domain record. */
    @Test
    void updateSyncsPeriodsInPlaceAndReturnsDomainRecord() throws Exception {
        //arrange
        var username = new Username("vet-jack");
        var vet = new VetEntity("vet-jack", "hash");
        var idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(vet, 42L);

        var locationEntity = new LocationEntity(vet, "Old Name", "Europe/Vienna");
        var existingPeriod = new OpeningPeriodEntity(locationEntity, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 0);
        locationEntity.getWeeklyPeriods().add(existingPeriod);

        LocationData updateData = new Location(null, "New Name", "Europe/London",
                List.of(new Location.OpeningPeriodResponse(1, LocalTime.of(8, 0), LocalTime.of(16, 0), 0)),
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
        assertThat(result.weeklyPeriods()).hasSize(1);
        assertThat(result.weeklyPeriods().get(0).startTime()).isEqualTo(LocalTime.of(8, 0));
        verify(repository).save(any(LocationEntity.class));
    }
}
