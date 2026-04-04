package tech.petclinix.web.dto;

import jakarta.validation.constraints.NotBlank;
import tech.petclinix.logic.domain.Gender;
import tech.petclinix.logic.domain.PetData;
import tech.petclinix.logic.domain.Species;

import java.time.LocalDate;

public record PetRequest(
        @NotBlank String name,
        Species species,
        Gender gender,
        LocalDate birthDate
) implements PetData {
}
