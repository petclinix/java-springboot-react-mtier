package tech.petclinix.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link LocationEntity}.
 *
 * Verifies that the {@code @OneToMany weeklyPeriods} and {@code @OneToMany overrides}
 * collections maintain the back-pointer to the owning location correctly in Java, and
 * that {@link LocationEntity#isOpenAt} evaluates date-specific overrides and weekly
 * periods correctly. No database involved.
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

    /** A closed override wins even when a weekly period would otherwise report open. */
    @Test
    void isOpenAtReturnsFalseWhenOverrideIsClosed() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var date = LocalDate.of(2025, 12, 25);
        location.getWeeklyPeriods().add(
                new OpeningPeriodEntity(location, date.getDayOfWeek().getValue(), LocalTime.of(0, 0), LocalTime.of(23, 59), 0));
        location.getOverrides().add(
                new OpeningOverrideEntity(location, date, null, null, true, "Christmas"));
        var instant = Instant.parse("2025-12-25T10:00:00Z");

        //act
        boolean open = location.isOpenAt(instant);

        //assert
        assertThat(open).isFalse();
    }

    /** An override that isn't closed but has no open/close time set is treated as closed. */
    @Test
    void isOpenAtReturnsFalseWhenOverrideHasNoTimesAndIsNotClosed() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var date = LocalDate.of(2025, 12, 26);
        location.getOverrides().add(
                new OpeningOverrideEntity(location, date, null, null, false, "Data entry incomplete"));
        var instant = Instant.parse("2025-12-26T10:00:00Z");

        //act
        boolean open = location.isOpenAt(instant);

        //assert
        assertThat(open).isFalse();
    }

    /** An override with special hours reports open for instants inside its window. */
    @Test
    void isOpenAtReturnsTrueWithinOverrideSpecialHours() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var date = LocalDate.of(2025, 12, 27);
        location.getOverrides().add(
                new OpeningOverrideEntity(location, date, LocalTime.of(9, 0), LocalTime.of(12, 0), false, "Half day"));
        var instant = Instant.parse("2025-12-27T10:00:00Z");

        //act
        boolean open = location.isOpenAt(instant);

        //assert
        assertThat(open).isTrue();
    }

    /** An override with special hours reports closed for instants outside its window. */
    @Test
    void isOpenAtReturnsFalseOutsideOverrideSpecialHours() {
        //arrange
        var vet = new VetEntity("vet-jack", "hash");
        var location = new LocationEntity(vet, "Clinic North", "UTC");
        var date = LocalDate.of(2025, 12, 27);
        location.getOverrides().add(
                new OpeningOverrideEntity(location, date, LocalTime.of(9, 0), LocalTime.of(12, 0), false, "Half day"));
        var instant = Instant.parse("2025-12-27T14:00:00Z");

        //act
        boolean open = location.isOpenAt(instant);

        //assert
        assertThat(open).isFalse();
    }
}
