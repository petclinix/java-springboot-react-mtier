package tech.petclinix.persistence.jpa;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;
import tech.petclinix.logic.service.UserType;

@Converter(autoApply = true)
public class UserTypeConverter implements AttributeConverter<UserType, String> {

    @Override
    public String convertToDatabaseColumn(UserType status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public UserType convertToEntityAttribute(String code) {
        return code != null ? UserType.fromCode(code) : null;
    }
}
