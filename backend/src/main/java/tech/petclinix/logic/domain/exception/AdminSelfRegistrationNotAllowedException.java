package tech.petclinix.logic.domain.exception;

public class AdminSelfRegistrationNotAllowedException extends PetclinixException {
    public AdminSelfRegistrationNotAllowedException() {
        super("Cannot self-register as ADMIN");
    }
}
