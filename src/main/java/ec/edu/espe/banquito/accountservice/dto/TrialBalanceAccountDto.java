package ec.edu.espe.banquito.accountservice.dto;

import java.math.BigDecimal;

/** Saldo deudor/acreedor de una cuenta dentro del Balance de Comprobación. */
public record TrialBalanceAccountDto(
        String code,
        String name,
        BigDecimal debitBalance,
        BigDecimal creditBalance) {
}
