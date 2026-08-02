package tech.petclinix.logic.domain.exception;

public class PetPictureTooLargeException extends PetclinixException {
    public PetPictureTooLargeException(int actualBytes, int maxBytes) {
        super("Pet picture size %d bytes exceeds the maximum allowed size of %d bytes".formatted(actualBytes, maxBytes));
    }
}
