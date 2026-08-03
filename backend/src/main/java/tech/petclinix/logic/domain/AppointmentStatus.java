package tech.petclinix.logic.domain;

/**
 * Lifecycle status of an {@link Appointment}.
 *
 * <p>Only the {@code BOOKED -> CANCELLED} transition has real workflow logic today.
 * {@code COMPLETED} and {@code NO_SHOW} are defined for future use but are not yet
 * reachable through any current use-case.
 */
public enum AppointmentStatus {
    BOOKED, CANCELLED, COMPLETED, NO_SHOW
}
