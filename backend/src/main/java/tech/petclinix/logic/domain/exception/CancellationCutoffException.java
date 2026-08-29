package tech.petclinix.logic.domain.exception;

/**
 * Thrown when an appointment is cancelled (or rescheduled) too close to its scheduled
 * start time — inside the mandatory cancellation cutoff window.
 */
public class CancellationCutoffException extends PetclinixException {
    public CancellationCutoffException(Long appointmentId, long cutoffHours) {
        super("Appointment %d can no longer be cancelled: within %d hour(s) of the scheduled start time"
                .formatted(appointmentId, cutoffHours));
    }
}
