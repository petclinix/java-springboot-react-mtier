package tech.petclinix.persistence.mapper;

import tech.petclinix.logic.domain.DomainPet;
import tech.petclinix.persistence.entity.PetEntity;

public class PetMapper {
    public static DomainPet toDomain(PetEntity e) {
        return new DomainPet(e.getId(), e.getName());
    }

}
