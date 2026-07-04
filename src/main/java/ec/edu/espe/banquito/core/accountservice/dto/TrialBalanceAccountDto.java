package ec.edu.espe.banquito.core.accountservice.dto;

import java.math.BigDecimal;

public record TrialBalanceAccountDto(
        String code,
        String name,
        BigDecimal debitBalance,
        BigDecimal creditBalance) {
}
