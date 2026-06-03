package ec.edu.espe.banquito.accountservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado del proceso End-of-Day (cierre de día contable). */
public record EodResponse(
        String eodStatus,
        LocalDate contableDateClosed,
        LocalDate nextContableDate,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        String balanceCheck,
        String reportPath) {
}
