package tech.petclinix.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PetRequest(@NotBlank String name) {
}
