package tech.petclinix.web.dto;

import java.time.LocalDateTime;

public record AppointmentResponse(Long id,Long vetId, Long petId, LocalDateTime startsAt) {
}
