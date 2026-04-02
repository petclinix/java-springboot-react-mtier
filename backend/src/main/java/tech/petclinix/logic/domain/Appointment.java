package tech.petclinix.logic.domain;

import java.time.LocalDateTime;

public record Appointment(Long id, Long vetId, Long petId, LocalDateTime startsAt) {
}
