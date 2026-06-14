package ec.edu.espe.banquito.accountservice.exception;

public class EodNotBalancedException extends AccountingException {

    public EodNotBalancedException(String message) {
        super(message);
    }
}
