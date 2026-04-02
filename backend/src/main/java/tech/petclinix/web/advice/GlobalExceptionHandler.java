package tech.petclinix.web.advice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.petclinix.logic.domain.exception.PetclinixException;
import tech.petclinix.logic.domain.exception.NotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(NotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(PetclinixException.class)
    public ResponseEntity<String> handleDomainException(PetclinixException ex) {
        return ResponseEntity.status(422).body(ex.getMessage());
    }
}
