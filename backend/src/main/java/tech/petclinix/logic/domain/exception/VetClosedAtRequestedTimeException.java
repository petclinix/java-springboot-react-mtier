package tech.petclinix.logic.domain.exception;

import java.time.LocalDateTime;

public class VetClosedAtRequestedTimeException extends PetclinixException {
    public VetClosedAtRequestedTimeException(String vetUsername, LocalDateTime startsAt) {
        super("Vet %s has no location open at %s".formatted(vetUsername, startsAt));
    }
}
