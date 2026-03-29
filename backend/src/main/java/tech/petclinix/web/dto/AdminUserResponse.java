package tech.petclinix.web.dto;

public record AdminUserResponse(Long id, String username, String role, boolean active) {
}
