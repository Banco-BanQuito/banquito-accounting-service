package ec.edu.espe.banquito.accountservice.exception;

public class UnbalancedEntryException extends AccountingException {

    public UnbalancedEntryException(String message) {
        super(message);
    }
}
