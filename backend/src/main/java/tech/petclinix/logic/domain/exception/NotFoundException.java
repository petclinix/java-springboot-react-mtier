package tech.petclinix.logic.domain.exception;

public class NotFoundException extends PetclinixException {
    public NotFoundException(String message) {
        super(message);
    }
}
