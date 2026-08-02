package tech.petclinix.logic.domain;

import java.time.LocalDateTime;

public record ActivityLogEntry(Long id, String username, String action, LocalDateTime timestamp) {
}
