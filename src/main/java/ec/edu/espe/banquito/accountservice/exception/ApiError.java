package ec.edu.espe.banquito.accountservice.exception;

import java.time.Instant;

/** Cuerpo de error uniforme para respuestas de la API. */
public record ApiError(
        int status,
        String error,
        String message,
        Instant timestamp) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now());
    }
}
