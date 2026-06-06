package ec.edu.espe.banquito.accountservice.dto;

import java.math.BigDecimal;

public record JournalEntryLineRequest(
        String accountCode,
        String movementType,
        BigDecimal amount,
        String reference) {
}
