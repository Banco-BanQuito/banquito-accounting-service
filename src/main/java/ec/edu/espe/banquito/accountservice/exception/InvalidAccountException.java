package ec.edu.espe.banquito.accountservice.exception;

/** Una línea referencia una cuenta inexistente o que no es de tipo DETALLE. HTTP 422. */
public class InvalidAccountException extends RuntimeException {

    public InvalidAccountException(String message) {
        super(message);
    }
}
