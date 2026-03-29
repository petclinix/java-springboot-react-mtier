package tech.petclinix.web.dto;

public record VetVisitResponse(Long id, String vetSummary, String ownerSummary, String vaccination) {
}
