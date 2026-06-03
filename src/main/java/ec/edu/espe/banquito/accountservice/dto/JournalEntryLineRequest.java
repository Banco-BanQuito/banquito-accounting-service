package ec.edu.espe.banquito.accountservice.dto;

import ec.edu.espe.banquito.accountservice.domain.MovementType;
import java.math.BigDecimal;

/** Línea entrante de un asiento contable. */
public record JournalEntryLineRequest(
        String accountCode,
        MovementType movementType,
        BigDecimal amount,
        String reference) {
}
