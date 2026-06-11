package ec.edu.espe.banquito.accountservice.controller;

import ec.edu.espe.banquito.accountservice.dto.EodRequest;
import ec.edu.espe.banquito.accountservice.dto.EodResponse;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.accountservice.service.AccountingService;
import ec.edu.espe.banquito.accountservice.service.EndOfDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/accounting")
@Tag(name = "Accounting", description = "Libro mayor contable: registro de asientos y Balance de Comprobación (RF-01, RF-09).")
public class AccountingController {

    private final AccountingService accountingService;
    private final EndOfDayService endOfDayService;

    public AccountingController(AccountingService accountingService, EndOfDayService endOfDayService) {
        this.accountingService = accountingService;
        this.endOfDayService = endOfDayService;
    }

    @PostMapping("/entries")
    @Operation(summary = "Registrar asiento contable", description = "Registra un asiento de partida doble. Valida que suma(DÉBITOS) == suma(CRÉDITOS); devuelve 422 si no cuadra.")
    @ApiResponse(responseCode = "200", description = "Asiento registrado")
    @ApiResponse(responseCode = "422", description = "El asiento no cuadra o cuenta inválida")
    public ResponseEntity<JournalEntryResponse> registerEntry(@RequestBody JournalEntryRequest request) {
        return ResponseEntity.ok(accountingService.registerEntry(request));
    }

    @PostMapping("/eod")
    @Operation(summary = "Proceso End-of-Day", description = "Cierra el día contable. Devuelve 200 si los débitos cuadran con los créditos, 409 si no cuadran.")
    @ApiResponse(responseCode = "200", description = "Cierre completado")
    @ApiResponse(responseCode = "409", description = "No cuadra el Balance de Comprobación")
    public ResponseEntity<EodResponse> runEndOfDay(@RequestBody(required = false) EodRequest request) {
        return ResponseEntity.ok(endOfDayService.runEndOfDay(request));
    }

    @GetMapping("/trial-balance")
    @Operation(summary = "Balance de Comprobación", description = "Devuelve los saldos deudores y acreedores por cuenta para la fecha indicada (o la fecha contable activa).")
    @ApiResponse(responseCode = "200", description = "Balance devuelto")
    public ResponseEntity<TrialBalanceResponse> trialBalance(
            @Parameter(description = "Fecha contable (YYYY-MM-DD). Opcional, default: fecha contable activa.", example = "2026-05-30")
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(accountingService.trialBalance(date));
    }
}
