package ec.edu.espe.banquito.accountservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de negocio a los códigos HTTP del contrato. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({UnbalancedEntryException.class, InvalidAccountException.class})
    public ResponseEntity<ApiError> handleUnprocessable(RuntimeException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(ApiError.of(HttpStatus.UNPROCESSABLE_ENTITY.value(), "UNPROCESSABLE_ENTITY", ex.getMessage()));
    }

    @ExceptionHandler(EodNotBalancedException.class)
    public ResponseEntity<ApiError> handleConflict(EodNotBalancedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage()));
    }
}
