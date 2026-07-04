package ec.edu.espe.banquito.core.accountservice.exception;

public class UnbalancedEntryException extends AccountingException {

    public UnbalancedEntryException(String message) {
        super(message);
    }
}
