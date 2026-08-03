package tech.petclinix.logic.domain.exception;

import java.time.LocalDateTime;

public class AppointmentOverlapException extends PetclinixException {
    public AppointmentOverlapException(Long vetId, LocalDateTime startAt, LocalDateTime endsAt) {
        super("Vet %d already has an appointment overlapping %s–%s".formatted(vetId, startAt, endsAt));
    }
}
