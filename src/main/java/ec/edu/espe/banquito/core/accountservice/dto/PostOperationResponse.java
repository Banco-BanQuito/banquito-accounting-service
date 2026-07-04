package ec.edu.espe.banquito.core.accountservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PostOperationResponse(
        Long entryId,
        String entryUuid,
        String status,
        String validationResult,
        LocalDateTime registeredAt,
        BigDecimal commissionAmount,
        BigDecimal ivaAmount,
        BigDecimal totalDebited) {
}
