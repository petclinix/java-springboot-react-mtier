package tech.petclinix.logic.domain.exception;

public class PetNameAlreadyInUseException extends PetclinixException {
    public PetNameAlreadyInUseException(String name) {
        super("Pet name '%s' is already in use for this owner".formatted(name));
    }
}
