package ec.edu.espe.banquito.accountservice.exception;

import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";

    @ExceptionHandler({UnbalancedEntryException.class, InvalidAccountException.class})
    public ResponseEntity<Map<String, String>> handleUnprocessable(AccountingException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(EodNotBalancedException.class)
    public ResponseEntity<Map<String, String>> handleConflict(EodNotBalancedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(AccountingValidationException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(AccountingValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<Map<String, String>> handleIoError(UncheckedIOException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }
}
