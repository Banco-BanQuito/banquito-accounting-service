package ec.edu.espe.banquito.core.accountservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EodResponse(
        String eodStatus,
        LocalDate contableDateClosed,
        LocalDate nextContableDate,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        String balanceCheck,
        String reportPath,
        List<CorrespondentBankPositionDto> correspondentBankPositions,
        String correspondentBankPositionReportPath) {
}
