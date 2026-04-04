package tech.petclinix.logic.domain;

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

}
