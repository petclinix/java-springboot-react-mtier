package tech.petclinix.logic.service;

public record DomainUser(Long id, String username, String passwordHash) {}
