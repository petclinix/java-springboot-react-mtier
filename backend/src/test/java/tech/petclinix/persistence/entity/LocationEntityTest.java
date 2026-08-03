package tech.petclinix.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link LocationEntity}.
 *
 * Verifies that the {@code @OneToMany weeklyPeriods} and {@code @OneToMany overrides}
 * collections maintain the back-pointer to the owning location correctly in Java.
 * No database involved.
 */
class LocationEntityTest {

    /** Adding a period to the collection is consistent with the period's back-pointer. */
    @Test
    void addingPeriodToCollectionMatchesItsBackPointer() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");
        var period = new OpeningPeriodEntity(location, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 0);

        //act
        location.getWeeklyPeriods().add(period);

        //assert
        assertThat(location.getWeeklyPeriods()).contains(period);
        assertThat(period.getLocation()).isSameAs(location);
    }

    /** Adding an override to the collection is consistent with the override's back-pointer. */
    @Test
    void addingOverrideToCollectionMatchesItsBackPointer() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "Europe/Vienna");
        var override = new OpeningOverrideEntity(location, LocalDate.of(2025, 12, 25),
                null, null, true, "Christmas");

        //act
        location.getOverrides().add(override);

        //assert
        assertThat(location.getOverrides()).contains(override);
        assertThat(override.getLocation()).isSameAs(location);
    }
}
