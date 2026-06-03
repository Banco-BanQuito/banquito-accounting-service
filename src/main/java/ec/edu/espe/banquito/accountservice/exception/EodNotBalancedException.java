package ec.edu.espe.banquito.accountservice.exception;

/** El cierre EOD no puede completarse porque los saldos no cuadran. HTTP 409. */
public class EodNotBalancedException extends RuntimeException {

    public EodNotBalancedException(String message) {
        super(message);
    }
}
