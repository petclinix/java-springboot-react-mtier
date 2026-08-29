package tech.petclinix.logic.domain;

/**
 * Lifecycle status of an {@link Appointment}.
 *
 * <p>Valid transitions:
 * <pre>
 *   BOOKED    -&gt; CONFIRMED   (vet confirms)
 *   BOOKED    -&gt; CANCELLED   (owner/vet cancels, before cutoff)
 *   CONFIRMED -&gt; CANCELLED   (owner/vet cancels, before cutoff)
 *   CONFIRMED -&gt; COMPLETED   (vet records the visit)
 *   CONFIRMED -&gt; NO_SHOW     (vet marks the owner as a no-show)
 * </pre>
 * Any transition not listed above is rejected. {@code CANCELLED}, {@code COMPLETED} and
 * {@code NO_SHOW} are terminal states.
 */
public enum AppointmentStatus {
    BOOKED, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW
}
