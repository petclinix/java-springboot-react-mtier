package tech.petclinix.logic.domain;

import java.time.LocalDate;

public record Pet(Long id, String name, String species, String gender, LocalDate birthDate) {
}
