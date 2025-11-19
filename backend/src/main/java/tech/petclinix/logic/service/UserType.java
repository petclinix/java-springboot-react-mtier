package tech.petclinix.logic.service;

public enum UserType {
    ADMIN("A"),
    VET("V"),
    OWNER("O");

    private final String code;

    UserType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static UserType fromCode(String code) {
        for (UserType s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("Unknown Status code " + code);
    }
}
