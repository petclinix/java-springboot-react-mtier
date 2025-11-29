package tech.petclinix.web.dto;

import java.time.LocalDate;

public record PetResponse(Long id, String name, String species, String gender, LocalDate birthDate) {
}
