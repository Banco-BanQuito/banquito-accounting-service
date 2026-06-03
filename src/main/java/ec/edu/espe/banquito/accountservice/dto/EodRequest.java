package ec.edu.espe.banquito.accountservice.dto;

import java.time.LocalDate;

/**
 * Petición del proceso End-of-Day.
 * {@code contableDate} es opcional: si llega null se cierra la FECHA_CONTABLE_ACTIVA.
 */
public record EodRequest(
        String requestedBy,
        LocalDate contableDate) {
}
