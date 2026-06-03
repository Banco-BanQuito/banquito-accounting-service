package ec.edu.espe.banquito.accountservice.controller;

import ec.edu.espe.banquito.accountservice.dto.EodRequest;
import ec.edu.espe.banquito.accountservice.dto.EodResponse;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.accountservice.service.AccountingService;
import ec.edu.espe.banquito.accountservice.service.EndOfDayService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints del servicio contable (RF-01 y RF-09 del Core). */
@RestController
@RequestMapping("/api/v2/accounting")
public class AccountingController {

    private final AccountingService accountingService;
    private final EndOfDayService endOfDayService;

    public AccountingController(AccountingService accountingService, EndOfDayService endOfDayService) {
        this.accountingService = accountingService;
        this.endOfDayService = endOfDayService;
    }

    /** Registra un asiento contable. Valida partida doble; 422 si no cuadra. */
    @PostMapping("/entries")
    public ResponseEntity<JournalEntryResponse> registerEntry(@RequestBody JournalEntryRequest request) {
        return ResponseEntity.ok(accountingService.registerEntry(request));
    }

    /** Proceso End-of-Day. 200 si cuadra, 409 si no cuadra. */
    @PostMapping("/eod")
    public ResponseEntity<EodResponse> runEndOfDay(@RequestBody(required = false) EodRequest request) {
        return ResponseEntity.ok(endOfDayService.runEndOfDay(request));
    }

    /** Balance de Comprobación del día (o de la fecha indicada). */
    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceResponse> trialBalance(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(accountingService.trialBalance(date));
    }
}
