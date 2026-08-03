package tech.petclinix.logic.domain.exception;

public class PetAlreadyDeactivatedException extends PetclinixException {
    public PetAlreadyDeactivatedException(Long petId) {
        super("Pet %d is already deactivated".formatted(petId));
    }
}
