package tech.petclinix.logic.domain;

public record DomainUser(Long id, String username, UserType userType, boolean active) {}
