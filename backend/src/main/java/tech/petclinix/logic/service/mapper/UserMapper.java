package tech.petclinix.logic.service.mapper;

import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.persistence.entity.AdminEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.entity.UserEntity.UserVisitor;
import tech.petclinix.persistence.entity.VetEntity;

public class UserMapper {

    public static DomainUser toDomain(UserEntity e) {
        return new DomainUser(e.getId(), e.getUsername(), getUserType(e), e.isActive());
    }

    public static UserType getUserType(UserEntity e) {
        return e.accept(new UserVisitor<>() {
            @Override
            public UserType visitOwner(OwnerEntity owner) {
                return UserType.OWNER;
            }

            @Override
            public UserType visitVet(VetEntity vet) {
                return UserType.VET;
            }

            @Override
            public UserType visitAdmin(AdminEntity admin) {
                return UserType.ADMIN;
            }
        });
    }

}
