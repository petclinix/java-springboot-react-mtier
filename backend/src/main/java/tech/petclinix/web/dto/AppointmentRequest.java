package tech.petclinix.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import tech.petclinix.logic.domain.AppointmentData;
import tech.petclinix.logic.domain.AppointmentType;
import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull Long locationId,
        @NotNull Long petId,
        @NotNull @Future LocalDateTime startsAt,
        @NotNull AppointmentType appointmentType
) implements AppointmentData {
}
