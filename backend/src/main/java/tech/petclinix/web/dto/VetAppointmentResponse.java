package tech.petclinix.web.dto;

import java.time.LocalDateTime;

public record VetAppointmentResponse(Long id, Long petId, String petName, String ownerUsername, LocalDateTime startsAt) {
}
