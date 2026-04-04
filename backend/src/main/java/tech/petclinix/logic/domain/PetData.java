package tech.petclinix.logic.domain;

import java.time.LocalDate;

/**
 * Contract for pet write operations accepted by {@link tech.petclinix.logic.service.PetService}.
 *
 * <p>The service accepts this interface, not a concrete DTO, so that the web layer can pass
 * its own type without the logic layer importing from {@code web}.
 */
public interface PetData {
    String name();
    Species species();
    Gender gender();
    LocalDate birthDate();
}
