package tech.petclinix.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import tech.petclinix.logic.domain.RescheduleData;

import java.time.LocalDateTime;

public record RescheduleRequest(
        @NotNull @Future LocalDateTime startsAt
) implements RescheduleData {
}
