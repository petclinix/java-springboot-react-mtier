package tech.petclinix.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentRequest(Long vetId, Long petId, LocalDateTime startsAt) {
}
