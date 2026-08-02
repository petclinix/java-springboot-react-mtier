package tech.petclinix.web.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(Long id, String username, String role, boolean active, LocalDateTime lastLogin) {
}
