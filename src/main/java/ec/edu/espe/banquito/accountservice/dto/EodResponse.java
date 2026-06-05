package ec.edu.espe.banquito.accountservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EodResponse(
        String eodStatus,
        LocalDate contableDateClosed,
        LocalDate nextContableDate,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        String balanceCheck,
        String reportPath) {
}
