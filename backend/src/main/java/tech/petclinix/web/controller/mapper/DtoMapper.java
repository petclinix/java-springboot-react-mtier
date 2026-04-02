package tech.petclinix.web.controller.mapper;

import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.web.dto.*;

public class DtoMapper {

    public static UserResponse toUserResponse(DomainUser user) {
        return new UserResponse(user.id(), user.username(), user.userType() == UserType.OWNER);
    }

    public static AdminUserResponse toAdminUserResponse(DomainUser user) {
        return new AdminUserResponse(
                user.id(),
                user.username(),
                user.userType().name(),
                user.active());
    }
}
