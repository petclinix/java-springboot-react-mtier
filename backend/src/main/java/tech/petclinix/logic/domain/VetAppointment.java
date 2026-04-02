package tech.petclinix.logic.domain;

import java.time.LocalDateTime;

public record VetAppointment(Long id, Long petId, String petName, String ownerUsername, LocalDateTime startsAt) {
}
