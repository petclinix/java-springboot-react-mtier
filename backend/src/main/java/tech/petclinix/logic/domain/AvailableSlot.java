package tech.petclinix.logic.domain;

import java.time.LocalDateTime;

/**
 * A single bookable window at a location, derived from its opening hours minus any active
 * (BOOKED or CONFIRMED) appointments already occupying that window. Both timestamps are plain,
 * local to the owning location's timezone (the same convention appointments themselves use),
 * not UTC-shifted.
 */
public record AvailableSlot(LocalDateTime startsAt, LocalDateTime endsAt) {
}
