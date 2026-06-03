package ec.edu.espe.banquito.accountservice.exception;

/** El asiento no cumple la regla de partida doble (suma DÉBITOS != suma CRÉDITOS). HTTP 422. */
public class UnbalancedEntryException extends RuntimeException {

    public UnbalancedEntryException(String message) {
        super(message);
    }
}
