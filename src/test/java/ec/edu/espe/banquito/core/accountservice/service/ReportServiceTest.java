package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceAccountDto;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    private final ReportService service = new ReportService();

    private TrialBalanceResponse balanceWithAccounts(boolean balanced) {
        List<TrialBalanceAccountDto> accounts = List.of(
                new TrialBalanceAccountDto("1.1.0.01", "Caja", new BigDecimal("100.00"), BigDecimal.ZERO));
        return new TrialBalanceResponse(LocalDate.of(2026, 5, 30), accounts,
                new BigDecimal("100.00"), new BigDecimal("100.00"), balanced);
    }

    @Test
    void generateCsvBytesIncludesHeaderAccountsAndTotals() {
        byte[] csv = service.generateCsvBytes(balanceWithAccounts(true));

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("Caja");
        assertThat(content).contains("TOTAL");
    }

    @Test
    void generatePdfBytesReturnsValidPdf() {
        byte[] pdf = service.generatePdfBytes(balanceWithAccounts(true));

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void generatePdfBytesHandlesUnbalancedTrialBalance() {
        byte[] pdf = service.generatePdfBytes(balanceWithAccounts(false));

        assertThat(pdf).isNotEmpty();
    }
}
