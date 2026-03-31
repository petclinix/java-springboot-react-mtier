package tech.petclinix.web.dto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.petclinix.logic.domain.UserType;

import java.io.IOException;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull
        @JsonDeserialize(using = UserTypeDeserializer.class)
        UserType type
) {

    public static class UserTypeDeserializer extends JsonDeserializer<UserType> {
        @Override
        public UserType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            String input = jsonParser.getText();
            if (input == null) return null;

            for (UserType userType : UserType.values()) {
                if (userType.name().equalsIgnoreCase(input)) {
                    return userType;
                }
            }
            throw new IllegalArgumentException(
                    "No enum constant for " + input);
        }
    }
}

