package tech.petclinix.web.dto;

import jakarta.validation.constraints.NotNull;
import tech.petclinix.logic.domain.AppointmentData;
import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull Long vetId,
        @NotNull Long petId,
        @NotNull LocalDateTime startsAt
) implements AppointmentData {
}
