package tech.petclinix.logic.domain.exception;

public class InvalidCredentialsException extends PetclinixException {
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
