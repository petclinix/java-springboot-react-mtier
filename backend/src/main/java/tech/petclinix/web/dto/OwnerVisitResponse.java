package tech.petclinix.web.dto;

import java.time.LocalDateTime;

public record OwnerVisitResponse(Long id, String ownerSummary, String vaccination, String vetUsername, LocalDateTime startsAt) {
}
