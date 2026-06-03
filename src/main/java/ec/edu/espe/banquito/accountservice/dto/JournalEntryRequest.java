package ec.edu.espe.banquito.accountservice.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Petición para registrar un asiento contable.
 * {@code entryDate} es opcional: si llega null se usa la FECHA_CONTABLE_ACTIVA.
 */
public record JournalEntryRequest(
        String entryUuid,
        String description,
        LocalDate entryDate,
        List<JournalEntryLineRequest> lines) {
}
