package tech.petclinix.web.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.domain.exception.PetclinixException;
import tech.petclinix.logic.domain.exception.UsernameAlreadyTakenException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(NotFoundException ex) {
        LOGGER.error("NotFoundException", ex);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ProblemDetail> handleUsernameAlreadyTaken(UsernameAlreadyTakenException ex) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(PetclinixException.class)
    public ResponseEntity<ProblemDetail> handlePetclinixException(PetclinixException ex) {
        LOGGER.error("PetclinixException", ex);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        LOGGER.error("AccessDeniedException", ex);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Not allowed to access this resource");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(detail);
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
//        if (ex instanceof ErrorResponse) {
//            return
//        }
//        LOGGER.error("Unexpected error", ex);
//        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail);
//    }
}
