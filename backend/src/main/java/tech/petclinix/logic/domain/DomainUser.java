package tech.petclinix.logic.domain;

import java.time.LocalDateTime;

public record DomainUser(Long id, String username, UserType userType, boolean active, LocalDateTime lastLogin) {}
