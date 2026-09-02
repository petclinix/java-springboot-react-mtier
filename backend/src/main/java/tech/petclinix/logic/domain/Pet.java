package tech.petclinix.logic.domain;

import java.time.LocalDate;
import java.util.Objects;

public record Pet(Long id, String name, String species, String breed, String gender, LocalDate birthDate, byte[] picture, String pictureContentType, boolean active) {

    @Override
    public boolean equals(Object o) {
        return o instanceof Pet other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Pet[id=%d, name=%s, species=%s, breed=%s, gender=%s, birthDate=%s, picture=<%d bytes>, pictureContentType=%s, active=%s]"
                .formatted(id, name, species, breed, gender, birthDate,
                        picture == null ? 0 : picture.length, pictureContentType, active);
    }
}
