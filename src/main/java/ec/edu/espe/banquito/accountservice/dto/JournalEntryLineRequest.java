package ec.edu.espe.banquito.accountservice.dto;

import ec.edu.espe.banquito.accountservice.domain.MovementType;
import java.math.BigDecimal;

public record JournalEntryLineRequest(
        String accountCode,
        MovementType movementType,
        BigDecimal amount,
        String reference) {
}
