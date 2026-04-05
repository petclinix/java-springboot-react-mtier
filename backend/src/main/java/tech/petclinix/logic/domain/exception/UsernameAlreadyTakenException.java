package tech.petclinix.logic.domain.exception;

public class UsernameAlreadyTakenException extends PetclinixException {
    public UsernameAlreadyTakenException(String username) {
        super("Username already taken: " + username);
    }
}
