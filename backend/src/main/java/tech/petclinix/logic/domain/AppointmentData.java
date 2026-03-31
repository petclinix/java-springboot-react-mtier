package tech.petclinix.logic.domain;

import java.time.LocalDateTime;

public interface AppointmentData {
    Long vetId();

    Long petId();

    LocalDateTime startsAt();
}
