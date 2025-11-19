package tech.petclinix.web.dto;

public record UserResponse(Long id, String username, boolean isOwner) {
}
