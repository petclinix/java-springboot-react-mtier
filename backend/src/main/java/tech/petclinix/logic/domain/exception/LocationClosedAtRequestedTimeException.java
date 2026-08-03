package tech.petclinix.logic.domain.exception;

import java.time.LocalDateTime;

public class LocationClosedAtRequestedTimeException extends PetclinixException {
    public LocationClosedAtRequestedTimeException(String locationName, LocalDateTime startsAt) {
        super("Location %s is not open at %s".formatted(locationName, startsAt));
    }
}
