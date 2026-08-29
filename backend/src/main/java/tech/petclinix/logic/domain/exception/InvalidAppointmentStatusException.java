package tech.petclinix.logic.domain.exception;

import tech.petclinix.logic.domain.AppointmentStatus;

/**
 * Thrown when an appointment lifecycle transition (confirm, no-show, complete via visit, ...)
 * is attempted while the appointment is not in the required source status.
 */
public class InvalidAppointmentStatusException extends PetclinixException {
    public InvalidAppointmentStatusException(Long appointmentId, AppointmentStatus expected, AppointmentStatus actual) {
        super("Appointment %d must be %s but is %s".formatted(appointmentId, expected, actual));
    }
}
