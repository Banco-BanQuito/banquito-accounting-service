package ec.edu.espe.banquito.core.accountservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;
import ec.edu.espe.banquito.core.accountservice.dto.CorrespondentBankPositionDto;
import ec.edu.espe.banquito.core.accountservice.dto.EodRequest;
import ec.edu.espe.banquito.core.accountservice.dto.EodResponse;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryDetailDto;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.core.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.core.accountservice.enums.CorrespondentBankStatus;
import ec.edu.espe.banquito.core.accountservice.repository.CorrespondentBankRepository;
import ec.edu.espe.banquito.core.accountservice.exception.EntryAlreadyReversedException;
import ec.edu.espe.banquito.core.accountservice.exception.EntryNotFoundException;
import ec.edu.espe.banquito.core.accountservice.exception.EodNotBalancedException;
import ec.edu.espe.banquito.core.accountservice.exception.InvalidAccountException;
import ec.edu.espe.banquito.core.accountservice.exception.UnbalancedEntryException;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.core.accountservice.service.AccountingAccountProvisioningService;
import ec.edu.espe.banquito.core.accountservice.service.AccountingRulesService;
import ec.edu.espe.banquito.core.accountservice.service.AccountingService;
import ec.edu.espe.banquito.core.accountservice.service.EndOfDayService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccountingServiceTest {

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private EndOfDayService endOfDayService;

    @Autowired
    private AccountingAccountRepository accountRepository;

    @Autowired
    private AccountingAccountProvisioningService provisioningService;

    @Autowired
    private AccountingRulesService rulesService;

    @Autowired
    private CorrespondentBankRepository correspondentBankRepository;

    private JournalEntryRequest depositoBalanceado(String uuid) {
        return new JournalEntryRequest(uuid, "Deposito ventanilla", null, List.of(
                new JournalEntryLineRequest("1.1.0.02", "DEBITO", new BigDecimal("500.00"), "DEP-001"),
                new JournalEntryLineRequest("2.1.0.01", "CREDITO", new BigDecimal("500.00"), "DEP-001")));
    }

    @Test
    void registraAsientoBalanceado() {
        JournalEntryResponse response = accountingService.registerEntry(depositoBalanceado("uuid-ok-001"));

        assertThat(response.status()).isEqualTo("REGISTRADO");
        assertThat(response.validationResult()).isEqualTo("SUMA_CERO_OK");
        assertThat(response.entryId()).isNotNull();
    }

    @Test
    void rechazaAsientoDescuadrado() {
        JournalEntryRequest descuadrado = new JournalEntryRequest("uuid-bad-001", "No cuadra", null, List.of(
                new JournalEntryLineRequest("1.1.0.02", "DEBITO", new BigDecimal("500.00"), null),
                new JournalEntryLineRequest("2.1.0.01", "CREDITO", new BigDecimal("400.00"), null)));

        assertThatThrownBy(() -> accountingService.registerEntry(descuadrado))
                .isInstanceOf(UnbalancedEntryException.class);
    }

    @Test
    void rechazaCuentaNoDetalle() {
        JournalEntryRequest estructural = new JournalEntryRequest("uuid-estructural-001", "Cuenta estructural", null, List.of(
                new JournalEntryLineRequest("1.0.0.00", "DEBITO", new BigDecimal("100.00"), null),
                new JournalEntryLineRequest("2.1.0.01", "CREDITO", new BigDecimal("100.00"), null)));

        assertThatThrownBy(() -> accountingService.registerEntry(estructural))
                .isInstanceOf(InvalidAccountException.class);
    }

    @Test
    void esIdempotentePorEntryUuid() {
        JournalEntryResponse first = accountingService.registerEntry(depositoBalanceado("uuid-idem-001"));
        JournalEntryResponse second = accountingService.registerEntry(depositoBalanceado("uuid-idem-001"));

        assertThat(second.entryId()).isEqualTo(first.entryId());
    }

    @Test
    void trialBalanceCuadraDeArranque() {
        TrialBalanceResponse balance = accountingService.trialBalance(null);

        assertThat(balance.accounts()).isNotEmpty();
        assertThat(balance.balanced()).isTrue();
        assertThat(balance.totalDebits()).isEqualByComparingTo(balance.totalCredits());
    }

    @Test
    void asientoBalanceadoMantieneElCuadre() {
        accountingService.registerEntry(depositoBalanceado("uuid-tb-001"));

        TrialBalanceResponse balance = accountingService.trialBalance(null);

        assertThat(balance.balanced()).isTrue();
    }

    @Test
    void structuralTrialBalanceCuadraConCuentasNostroYVostroPobladas() {
        CorrespondentBank pichincha = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        CorrespondentBank guayaquil = provisioningService.provisionCorrespondentBank("010", "Banco de Guayaquil");

        TrialBalanceResponse structuralAntes = accountingService.structuralTrialBalance(null);
        var activoRaizAntes = structuralAntes.accounts().stream()
                .filter(row -> "1.0.0.00".equals(row.code())).findFirst().orElseThrow();
        var pasivoRaizAntes = structuralAntes.accounts().stream()
                .filter(row -> "2.0.0.00".equals(row.code())).findFirst().orElseThrow();

        // Pago saliente: débito Nostro Pichincha / crédito Bóveda (liquidación bruta por transacción).
        accountingService.registerEntry(new JournalEntryRequest("uuid-nostro-001", "Pago saliente a Pichincha", null, List.of(
                new JournalEntryLineRequest(pichincha.getNostroAccountCode(), "DEBITO", new BigDecimal("1000.00"), null),
                new JournalEntryLineRequest("1.1.0.02", "CREDITO", new BigDecimal("1000.00"), null))));

        // Crédito entrante (documento de diseño): la cuenta Vostro actúa como cuenta
        // puente y se DEBITA (mismo patrón que BATCH_CREDIT_SAVINGS con BANCO_CENTRAL),
        // mientras se ACREDITA la cuenta del cliente. Como Vostro es PASIVO/ACREEDORA,
        // un DEBITO la mueve por debajo de su saldo inicial (0.00): -300.00 es el saldo
        // correcto para una cuenta puente en posición deficitaria hasta que se liquide
        // con el banco corresponsal real.
        accountingService.registerEntry(new JournalEntryRequest("uuid-vostro-001", "Crédito entrante de Guayaquil", null, List.of(
                new JournalEntryLineRequest(guayaquil.getVostroAccountCode(), "DEBITO", new BigDecimal("300.00"), null),
                new JournalEntryLineRequest("2.1.0.01", "CREDITO", new BigDecimal("300.00"), null))));

        TrialBalanceResponse structural = accountingService.structuralTrialBalance(null);

        assertThat(structural.balanced()).isTrue();
        assertThat(structural.totalDebits()).isEqualByComparingTo(structural.totalCredits());

        AccountingAccount nostro = accountRepository.findById(pichincha.getNostroAccountCode()).orElseThrow();
        AccountingAccount vostro = accountRepository.findById(guayaquil.getVostroAccountCode()).orElseThrow();
        assertThat(nostro.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(vostro.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("-300.00"));

        var activoRaiz = structural.accounts().stream()
                .filter(row -> "1.0.0.00".equals(row.code())).findFirst().orElseThrow();
        var pasivoRaiz = structural.accounts().stream()
                .filter(row -> "2.0.0.00".equals(row.code())).findFirst().orElseThrow();

        // Nostro (+1000.00) y Bóveda (-1000.00, misma línea opuesta del asiento) son ambas
        // ACTIVO/DEUDORA: se compensan exactamente dentro de la raíz 1.0.0.00. Igual con
        // Vostro (-300.00) y Ahorros del cliente (+300.00) en 2.0.0.00. La prueba real de
        // que las cuentas Nostro/Vostro no rompen el cuadre es que balanced()=true arriba
        // (structuralTrialBalance() sigue agrupando por accountClass sin cambios), no que
        // estas raíces puntuales cambien de valor — por diseño una liquidación bruta
        // interna mueve saldo entre cuentas de la MISMA clase, no cambia el total de la clase.
        // Se comparan deltas (no ceros absolutos) porque el plan de cuentas de producción
        // trae saldos de apertura distintos de cero.
        assertThat(activoRaiz.debitBalance()).isEqualByComparingTo(activoRaizAntes.debitBalance());
        assertThat(pasivoRaiz.creditBalance()).isEqualByComparingTo(pasivoRaizAntes.creditBalance());
    }

    @Test
    void structuralTrialBalanceCuadraTrasPoblarNostroVostroViaReglasFase3() {
        CorrespondentBank pichincha = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        CorrespondentBank guayaquil = provisioningService.provisionCorrespondentBank("010", "Banco de Guayaquil");

        rulesService.postOperation(new OperationRequest(
                "uuid-f3-nostro-out-001", "NOSTRO_SETTLEMENT_OUTBOUND", null, null,
                "1000.00", null, "NOUT-001", null, null, pichincha.getBankCode()));

        rulesService.postOperation(new OperationRequest(
                "uuid-f3-vostro-in-001", "VOSTRO_SETTLEMENT_INBOUND", null, null,
                "600.00", null, "VIN-001", null, null, guayaquil.getBankCode()));

        rulesService.postOperation(new OperationRequest(
                "uuid-f3-vostro-del-001", "VOSTRO_SETTLEMENT_DELIVERY", "CHECKING", null,
                "600.00", null, "VDEL-001", null, null, guayaquil.getBankCode()));

        TrialBalanceResponse structural = accountingService.structuralTrialBalance(null);

        assertThat(structural.balanced()).isTrue();
        assertThat(structural.totalDebits()).isEqualByComparingTo(structural.totalCredits());

        AccountingAccount nostro = accountRepository.findById(pichincha.getNostroAccountCode()).orElseThrow();
        AccountingAccount vostro = accountRepository.findById(guayaquil.getVostroAccountCode()).orElseThrow();
        assertThat(nostro.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(vostro.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void positionByCorrespondentBankCalculaNetPositionConNostroYVostroSimultaneos() {
        CorrespondentBank pichincha = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        CorrespondentBank guayaquil = provisioningService.provisionCorrespondentBank("010", "Banco de Guayaquil");
        provisioningService.provisionCorrespondentBank("017", "Produbanco");

        // Pichincha: solo Nostro (+1000.00), Vostro nunca se mueve -> netPosition = 1000.00.
        rulesService.postOperation(new OperationRequest(
                "uuid-pos-nostro-001", "NOSTRO_SETTLEMENT_OUTBOUND", null, null,
                "1000.00", null, "NOUT-P-001", null, null, pichincha.getBankCode()));

        // Guayaquil: Nostro (+500.00) y Vostro (+300.00, aún no entregado al cliente) simultáneos,
        // ambos != 0 -> caso no trivial de la fórmula: netPosition = 500.00 - 300.00 = 200.00.
        rulesService.postOperation(new OperationRequest(
                "uuid-pos-nostro-002", "NOSTRO_SETTLEMENT_OUTBOUND", null, null,
                "500.00", null, "NOUT-G-001", null, null, guayaquil.getBankCode()));
        rulesService.postOperation(new OperationRequest(
                "uuid-pos-vostro-001", "VOSTRO_SETTLEMENT_INBOUND", null, null,
                "300.00", null, "VIN-G-001", null, null, guayaquil.getBankCode()));

        // Produbanco: sin ningún movimiento -> debe aparecer con Nostro=0, Vostro=0, netPosition=0.

        List<CorrespondentBankPositionDto> positions = accountingService.positionByCorrespondentBank(null);

        var pichinchaRow = positions.stream()
                .filter(p -> "002".equals(p.bankCode())).findFirst().orElseThrow();
        assertThat(pichinchaRow.nostroBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(pichinchaRow.vostroBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pichinchaRow.netPosition()).isEqualByComparingTo(new BigDecimal("1000.00"));

        var guayaquilRow = positions.stream()
                .filter(p -> "010".equals(p.bankCode())).findFirst().orElseThrow();
        assertThat(guayaquilRow.nostroBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(guayaquilRow.vostroBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(guayaquilRow.netPosition()).isEqualByComparingTo(new BigDecimal("200.00"));

        var produbancoRow = positions.stream()
                .filter(p -> "017".equals(p.bankCode())).findFirst().orElseThrow();
        assertThat(produbancoRow.nostroBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(produbancoRow.vostroBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(produbancoRow.netPosition()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void positionByCorrespondentBankNoExcluyeBancoInactivoConSaldoResidual() {
        CorrespondentBank austro = provisioningService.provisionCorrespondentBank("034", "Banco del Austro");
        rulesService.postOperation(new OperationRequest(
                "uuid-pos-inactivo-001", "NOSTRO_SETTLEMENT_OUTBOUND", null, null,
                "750.00", null, "NOUT-A-001", null, null, austro.getBankCode()));

        austro.setStatus(CorrespondentBankStatus.INACTIVO);
        correspondentBankRepository.save(austro);

        List<CorrespondentBankPositionDto> positions = accountingService.positionByCorrespondentBank(null);

        var austroRow = positions.stream()
                .filter(p -> "034".equals(p.bankCode())).findFirst().orElseThrow();
        assertThat(austroRow.status()).isEqualTo("INACTIVO");
        assertThat(austroRow.nostroBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(austroRow.netPosition()).isEqualByComparingTo(new BigDecimal("750.00"));
    }

    @Test
    void eodCierraYAvanzaLaFecha() {
        EodResponse eod = endOfDayService.runEndOfDay(new EodRequest("system", null));

        assertThat(eod.balanceCheck()).isEqualTo("CUADRADO");
        assertThat(eod.eodStatus()).isEqualTo("COMPLETADO");
        assertThat(eod.nextContableDate()).isEqualTo(eod.contableDateClosed().plusDays(1));
    }

    @Test
    void eodGeneraCsvDePosicionPorBancoJuntoAlBalanceExistente() throws IOException {
        CorrespondentBank pichincha = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        CorrespondentBank guayaquil = provisioningService.provisionCorrespondentBank("010", "Banco de Guayaquil");
        provisioningService.provisionCorrespondentBank("017", "Produbanco");

        rulesService.postOperation(new OperationRequest(
                "uuid-eod-pos-nostro-001", "NOSTRO_SETTLEMENT_OUTBOUND", null, null,
                "1200.00", null, "NOUT-EOD-001", null, null, pichincha.getBankCode()));
        rulesService.postOperation(new OperationRequest(
                "uuid-eod-pos-vostro-001", "VOSTRO_SETTLEMENT_INBOUND", null, null,
                "400.00", null, "VIN-EOD-001", null, null, guayaquil.getBankCode()));

        EodResponse eod = endOfDayService.runEndOfDay(new EodRequest("system", null));

        // Regresión: el CSV del trial balance existente conserva exactamente su formato
        // (BOM + CRLF + encabezado en español + fila TOTAL), sin alterar una sola columna.
        String balanceCsv = Files.readString(Path.of(eod.reportPath()), StandardCharsets.UTF_8);
        assertThat(balanceCsv).startsWith("﻿" + "Código de Cuenta,Nombre de Cuenta,Saldo Deudor,Saldo Acreedor\r\n");
        assertThat(balanceCsv).contains("\r\nTOTAL,\"\",");

        // El archivo de posición por banco es adicional y separado, con su propio encabezado.
        assertThat(eod.correspondentBankPositionReportPath()).isNotBlank();
        String positionCsv = Files.readString(Path.of(eod.correspondentBankPositionReportPath()), StandardCharsets.UTF_8);
        assertThat(positionCsv).startsWith(
                "﻿" + "Código de Banco,Nombre de Banco,Estado,Saldo Nostro,Saldo Vostro,Posición Neta\r\n");
        assertThat(positionCsv).contains("002,\"Banco Pichincha\",ACTIVO,1200.00,0,1200.00");
        assertThat(positionCsv).contains("010,\"Banco de Guayaquil\",ACTIVO,0,400.00,-400.00");
        assertThat(positionCsv).contains("017,\"Produbanco\",ACTIVO,0,0,0");

        // El reporte de posición es informativo: no participa en el cuadre del EOD.
        assertThat(eod.balanceCheck()).isEqualTo("CUADRADO");
        assertThat(eod.correspondentBankPositions()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void eodNoCierraSiNoCuadra() {
        AccountingAccount boveda = accountRepository.findById("1.1.0.02").orElseThrow();
        boveda.setCurrentBalance(boveda.getCurrentBalance().add(new BigDecimal("999.99")));
        accountRepository.save(boveda);

        EodRequest req = new EodRequest("system", null);
        assertThatThrownBy(() -> endOfDayService.runEndOfDay(req))
                .isInstanceOf(EodNotBalancedException.class);
    }

    @Test
    void reversaAsientoInvierteMovimientosYAnulaElOriginal() {
        accountingService.registerEntry(depositoBalanceado("uuid-rev-001"));
        AccountingAccount boveda = accountRepository.findById("1.1.0.02").orElseThrow();
        BigDecimal balanceDespuesDelDeposito = boveda.getCurrentBalance();

        JournalEntryDetailDto reversal = accountingService.reverseEntry("uuid-rev-001");

        assertThat(reversal.status()).isEqualTo("REGISTRADO");
        assertThat(reversal.reversalOfEntryUuid()).isEqualTo("uuid-rev-001");
        assertThat(reversal.balanced()).isTrue();

        JournalEntryDetailDto original = accountingService.getEntryDetail("uuid-rev-001");
        assertThat(original.status()).isEqualTo("ANULADO");
        assertThat(original.reversedByEntryUuid()).isEqualTo(reversal.entryUuid());

        AccountingAccount bovedaFinal = accountRepository.findById("1.1.0.02").orElseThrow();
        assertThat(bovedaFinal.getCurrentBalance())
                .isEqualByComparingTo(balanceDespuesDelDeposito.subtract(new BigDecimal("500.00")));
    }

    @Test
    void rechazaRevertirUnAsientoYaReversado() {
        accountingService.registerEntry(depositoBalanceado("uuid-rev-002"));
        accountingService.reverseEntry("uuid-rev-002");

        assertThatThrownBy(() -> accountingService.reverseEntry("uuid-rev-002"))
                .isInstanceOf(EntryAlreadyReversedException.class);
    }

    @Test
    void rechazaRevertirUnAsientoInexistente() {
        assertThatThrownBy(() -> accountingService.reverseEntry("uuid-no-existe"))
                .isInstanceOf(EntryNotFoundException.class);
    }

    @Test
    void listaAsientosFiltradosPorEstado() {
        accountingService.registerEntry(depositoBalanceado("uuid-list-001"));
        accountingService.registerEntry(depositoBalanceado("uuid-list-002"));
        accountingService.reverseEntry("uuid-list-001");

        Page<JournalEntryDetailDto> anulados = accountingService.listEntries(
                null, null, "ANULADO", null, PageRequest.of(0, 20));

        assertThat(anulados.getContent())
                .extracting(JournalEntryDetailDto::entryUuid)
                .contains("uuid-list-001");
        assertThat(anulados.getContent())
                .extracting(JournalEntryDetailDto::entryUuid)
                .doesNotContain("uuid-list-002");
    }
}
