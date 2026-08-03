package tech.petclinix.logic.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link OpeningHours}.
 *
 * Verifies that {@link OpeningHours#isOpenAt} evaluates date-specific overrides
 * and weekly periods correctly. Plain JUnit 5, no Spring/H2/Mockito.
 */
class OpeningHoursTest {

    /** A closed override wins even when a weekly period would otherwise report open. */
    @Test
    void isOpenAtReturnsFalseWhenOverrideIsClosed() {
        //arrange
        var date = LocalDate.of(2025, 12, 25);
        var openingHours = new OpeningHours(
                "UTC",
                List.of(new OpeningHours.WeeklyPeriod(date.getDayOfWeek().getValue(), LocalTime.of(0, 0), LocalTime.of(23, 59), 0)),
                List.of(new OpeningHours.OpeningOverride(date, null, null, true, "Christmas")));

        //act
        boolean open = openingHours.isOpenAt(Instant.parse("2025-12-25T10:00:00Z"));

        //assert
        assertThat(open).isFalse();
    }

    /** An override that isn't closed but has no open/close time set is treated as closed. */
    @Test
    void isOpenAtReturnsFalseWhenOverrideHasNoTimesAndIsNotClosed() {
        //arrange
        var date = LocalDate.of(2025, 12, 26);
        var openingHours = new OpeningHours(
                "UTC",
                List.of(),
                List.of(new OpeningHours.OpeningOverride(date, null, null, false, "Data entry incomplete")));

        //act
        boolean open = openingHours.isOpenAt(Instant.parse("2025-12-26T10:00:00Z"));

        //assert
        assertThat(open).isFalse();
    }

    /** An override with special hours reports open for instants inside its window. */
    @Test
    void isOpenAtReturnsTrueWithinOverrideSpecialHours() {
        //arrange
        var date = LocalDate.of(2025, 12, 27);
        var openingHours = new OpeningHours(
                "UTC",
                List.of(),
                List.of(new OpeningHours.OpeningOverride(date, LocalTime.of(9, 0), LocalTime.of(12, 0), false, "Half day")));

        //act
        boolean open = openingHours.isOpenAt(Instant.parse("2025-12-27T10:00:00Z"));

        //assert
        assertThat(open).isTrue();
    }

    /** An override with special hours reports closed for instants outside its window. */
    @Test
    void isOpenAtReturnsFalseOutsideOverrideSpecialHours() {
        //arrange
        var date = LocalDate.of(2025, 12, 27);
        var openingHours = new OpeningHours(
                "UTC",
                List.of(),
                List.of(new OpeningHours.OpeningOverride(date, LocalTime.of(9, 0), LocalTime.of(12, 0), false, "Half day")));

        //act
        boolean open = openingHours.isOpenAt(Instant.parse("2025-12-27T14:00:00Z"));

        //assert
        assertThat(open).isFalse();
    }
}
