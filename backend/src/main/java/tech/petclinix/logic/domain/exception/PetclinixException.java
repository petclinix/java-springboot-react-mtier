package tech.petclinix.logic.domain.exception;

public abstract class PetclinixException extends RuntimeException {
    protected PetclinixException(String message) {
        super(message);
    }
}
