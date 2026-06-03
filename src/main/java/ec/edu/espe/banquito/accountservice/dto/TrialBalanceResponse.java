package ec.edu.espe.banquito.accountservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Balance de Comprobación de una fecha contable. */
public record TrialBalanceResponse(
        LocalDate contableDate,
        List<TrialBalanceAccountDto> accounts,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        boolean balanced) {
}
