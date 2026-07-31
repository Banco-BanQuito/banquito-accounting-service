package ec.edu.espe.banquito.core.accountservice.controller;

import ec.edu.espe.banquito.core.accountservice.dto.ActiveContableDateResponse;
import ec.edu.espe.banquito.core.accountservice.dto.EodRequest;
import ec.edu.espe.banquito.core.accountservice.dto.EodResponse;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryDetailDto;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.core.accountservice.service.AccountingService;
import ec.edu.espe.banquito.core.accountservice.service.EndOfDayService;
import ec.edu.espe.banquito.core.accountservice.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountingControllerTest {

    @Mock
    private AccountingService accountingService;
    @Mock
    private EndOfDayService endOfDayService;
    @Mock
    private ReportService reportService;

    private AccountingController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountingController(accountingService, endOfDayService, reportService);
    }

    @Test
    void registerEntryDelegatesToAccountingService() {
        JournalEntryRequest request = mock(JournalEntryRequest.class);
        JournalEntryResponse expected = mock(JournalEntryResponse.class);
        when(accountingService.registerEntry(request)).thenReturn(expected);

        assertThat(controller.registerEntry(request).getBody()).isSameAs(expected);
    }

    @Test
    void runEndOfDayDelegatesToEndOfDayService() {
        EodRequest request = mock(EodRequest.class);
        EodResponse expected = mock(EodResponse.class);
        when(endOfDayService.runEndOfDay(request)).thenReturn(expected);

        assertThat(controller.runEndOfDay(request).getBody()).isSameAs(expected);
    }

    @Test
    void getEntryDelegatesToAccountingService() {
        JournalEntryDetailDto expected = mock(JournalEntryDetailDto.class);
        when(accountingService.getEntryDetail("uuid-1")).thenReturn(expected);

        assertThat(controller.getEntry("uuid-1").getBody()).isSameAs(expected);
    }

    @Test
    void activeContableDateDelegatesToAccountingService() {
        when(accountingService.getActiveContableDate()).thenReturn(LocalDate.of(2026, 5, 30));

        ActiveContableDateResponse response = controller.activeContableDate().getBody();

        assertThat(response.contableDate()).isEqualTo(LocalDate.of(2026, 5, 30));
    }

    @Test
    void downloadCsvReturnsCsvAttachmentWithDateInFilename() {
        LocalDate date = LocalDate.of(2026, 5, 30);
        TrialBalanceResponse balance = new TrialBalanceResponse(date, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, true);
        byte[] csv = "cuenta,debe,haber\n".getBytes();
        when(accountingService.structuralTrialBalance(date)).thenReturn(balance);
        when(reportService.generateCsvBytes(balance)).thenReturn(csv);

        ResponseEntity<byte[]> response = controller.downloadCsv(date);

        assertThat(response.getBody()).isEqualTo(csv);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("balance_2026-05-30.csv");
    }

    @Test
    void downloadPdfReturnsPdfAttachment() {
        LocalDate date = LocalDate.of(2026, 5, 30);
        TrialBalanceResponse balance = new TrialBalanceResponse(date, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, true);
        byte[] pdf = "%PDF-1.4".getBytes();
        when(accountingService.structuralTrialBalance(date)).thenReturn(balance);
        when(reportService.generatePdfBytes(balance)).thenReturn(pdf);

        ResponseEntity<byte[]> response = controller.downloadPdf(date);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
    }
}
