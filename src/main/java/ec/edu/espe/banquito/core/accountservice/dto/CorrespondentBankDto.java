package ec.edu.espe.banquito.core.accountservice.dto;

import java.time.LocalDateTime;

public record CorrespondentBankDto(
        String bankCode,
        String bankName,
        String nostroAccountCode,
        String vostroAccountCode,
        String currency,
        String status,
        LocalDateTime creationDate) {
}
