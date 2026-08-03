package tech.petclinix.logic.domain.exception;

public class AppointmentAlreadyCancelledException extends PetclinixException {
    public AppointmentAlreadyCancelledException(Long appointmentId) {
        super("Appointment %d is already cancelled".formatted(appointmentId));
    }
}
