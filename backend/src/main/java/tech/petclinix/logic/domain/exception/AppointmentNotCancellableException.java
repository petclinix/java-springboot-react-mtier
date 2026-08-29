package tech.petclinix.logic.domain.exception;

/**
 * Thrown when an appointment is cancelled while it is not in a cancellable state,
 * i.e. its status is not {@code BOOKED} or {@code CONFIRMED}.
 */
public class AppointmentNotCancellableException extends PetclinixException {
    public AppointmentNotCancellableException(Long appointmentId) {
        super("Appointment %d is not in a cancellable state".formatted(appointmentId));
    }
}
