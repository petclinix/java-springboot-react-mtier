package tech.petclinix.logic.domain;

import java.time.LocalDateTime;

public record OwnerVisit(Long id, String ownerSummary, String vaccination, String vetUsername, LocalDateTime startsAt) {
}
