package ec.edu.espe.banquito.core.accountservice.dto;

import java.math.BigDecimal;

public record JournalEntryLineDto(
        String accountCode,
        String accountName,
        String movementType,
        BigDecimal amount,
        String reference) {
}
