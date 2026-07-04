package ec.edu.espe.banquito.core.accountservice.exception;

public class EodNotBalancedException extends AccountingException {

    public EodNotBalancedException(String message) {
        super(message);
    }
}
