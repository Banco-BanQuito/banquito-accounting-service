package ec.edu.espe.banquito.accountservice.exception;

public class EodNotBalancedException extends RuntimeException {

    public EodNotBalancedException(String message) {
        super(message);
    }
}
