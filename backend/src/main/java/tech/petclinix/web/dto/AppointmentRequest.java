package tech.petclinix.web.dto;

import tech.petclinix.logic.domain.AppointmentData;

import java.time.LocalDateTime;

public record AppointmentRequest(Long vetId, Long petId, LocalDateTime startsAt) implements AppointmentData {
}
