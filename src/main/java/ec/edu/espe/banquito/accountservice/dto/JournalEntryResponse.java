package ec.edu.espe.banquito.accountservice.dto;

import java.time.Instant;

/** Respuesta tras registrar un asiento contable. */
public record JournalEntryResponse(
        Long entryId,
        String entryUuid,
        String status,
        String validationResult,
        Instant registeredAt) {
}
