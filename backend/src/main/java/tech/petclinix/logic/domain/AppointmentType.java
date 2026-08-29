package tech.petclinix.logic.domain;

/**
 * The booking-time category of an {@link Appointment}, determining how long the appointment
 * slot needs to be. Not to be confused with the vet's post-appointment diagnosis/notes record
 * ({@link VetVisit}) — this is purely a duration-driving classification chosen at booking time.
 */
public enum AppointmentType {
    VACCINATION(15),
    FOLLOW_UP(20),
    CHECKUP(30),
    EMERGENCY(45),
    SURGERY(60);

    private final int durationMinutes;

    AppointmentType(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int durationMinutes() {
        return durationMinutes;
    }
}
