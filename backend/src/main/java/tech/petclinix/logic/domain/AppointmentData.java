package tech.petclinix.logic.domain;

import java.time.LocalDateTime;

public interface AppointmentData {
    Long locationId();

    Long petId();

    LocalDateTime startsAt();
}
