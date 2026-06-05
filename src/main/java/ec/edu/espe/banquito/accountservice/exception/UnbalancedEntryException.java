package ec.edu.espe.banquito.accountservice.exception;

public class UnbalancedEntryException extends RuntimeException {

    public UnbalancedEntryException(String message) {
        super(message);
    }
}
