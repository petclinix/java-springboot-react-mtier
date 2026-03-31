package tech.petclinix.web.dto;

import tech.petclinix.logic.domain.VetVisitData;

public record VetVisitRequest(String vetSummary, String ownerSummary, String vaccination) implements VetVisitData {
}
