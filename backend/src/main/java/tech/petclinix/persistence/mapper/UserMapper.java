package tech.petclinix.persistence.mapper;

import tech.petclinix.logic.service.DomainUser;
import tech.petclinix.persistence.entity.UserEntity;

public class UserMapper {
    public static DomainUser toDomain(UserEntity e) {
        return new DomainUser(e.getId(), e.getUsername(), e.getPasswordHash());
    }

    public static UserEntity toEntity(DomainUser d) {
        // don't set id - JPA will handle it for new entities
        return new UserEntity(d.username(), d.passwordHash());
    }
}
