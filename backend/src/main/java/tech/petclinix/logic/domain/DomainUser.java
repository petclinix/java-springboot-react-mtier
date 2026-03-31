package tech.petclinix.logic.domain;

public record DomainUser(Long id, String username, String passwordHash, UserType userType, boolean active) {}
