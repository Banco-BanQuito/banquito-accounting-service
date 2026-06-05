package ec.edu.espe.banquito.accountservice.dto;

import java.time.LocalDateTime;

public record JournalEntryResponse(
        Long entryId,
        String entryUuid,
        String status,
        String validationResult,
        LocalDateTime registeredAt) {
}
