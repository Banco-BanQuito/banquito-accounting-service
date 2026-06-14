package ec.edu.espe.banquito.accountservice.exception;

import java.io.UncheckedIOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({UnbalancedEntryException.class, InvalidAccountException.class})
    public ProblemDetail handleUnprocessable(AccountingException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(EodNotBalancedException.class)
    public ProblemDetail handleConflict(EodNotBalancedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccountingValidationException.class)
    public ProblemDetail handleBadRequest(AccountingValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ProblemDetail handleIoError(UncheckedIOException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
}
