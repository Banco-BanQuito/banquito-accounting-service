package ec.edu.espe.banquito.core.accountservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.espe.banquito.core.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.core.accountservice.dto.PostOperationResponse;
import ec.edu.espe.banquito.core.accountservice.enums.CorrespondentBankStatus;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.core.accountservice.repository.CorrespondentBankRepository;
import ec.edu.espe.banquito.core.accountservice.service.AccountingAccountProvisioningService;
import ec.edu.espe.banquito.core.accountservice.service.AccountingRulesService;
import ec.edu.espe.banquito.core.accountservice.service.AccountingService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccountingRulesServiceTest {

    @Autowired
    private AccountingRulesService rulesService;

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private AccountingAccountProvisioningService provisioningService;

    @Autowired
    private CorrespondentBankRepository correspondentBankRepository;

    @Autowired
    private AccountingAccountRepository accountRepository;

    @Test
    void tellerDepositRegistraAsientoBalanceado() {
        OperationRequest req = new OperationRequest(
                "uuid-rd-001", "TELLER_DEPOSIT", "SAVINGS", null, "500.00", "", "DEP-001", "");

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.validationResult()).isEqualTo("SUMA_CERO_OK");
        assertThat(resp.entryId()).isNotNull();
        assertThat(resp.commissionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.ivaAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.totalDebited()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void tellerWithdrawalRegistraAsientoBalanceado() {
        OperationRequest req = new OperationRequest(
                "uuid-rw-001", "TELLER_WITHDRAWAL", "SAVINGS", null, "200.00", null, "WIT-001", null);

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.totalDebited()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void postOperationEsIdempotentePorUuid() {
        OperationRequest req = new OperationRequest(
                "uuid-rd-idem-001", "TELLER_DEPOSIT", "SAVINGS", null, "300.00", null, "REF", null);

        PostOperationResponse first = rulesService.postOperation(req);
        PostOperationResponse second = rulesService.postOperation(req);

        assertThat(second.entryId()).isEqualTo(first.entryId());
    }

    @Test
    void p2pMismoTipoConComisionRegistraTodasLasLineas() {
        OperationRequest req = new OperationRequest(
                "uuid-p2p-001", "P2P_TRANSFER", "SAVINGS", null, "300.00", "5.00", "P2P-001", "");

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.commissionAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(resp.ivaAmount()).isEqualByComparingTo(new BigDecimal("0.75"));
        assertThat(resp.totalDebited()).isEqualByComparingTo(new BigDecimal("305.75"));
    }

    @Test
    void p2pMismoTipoSinComisionOmiteLineasComisionYSigueCuadrando() {
        OperationRequest req = new OperationRequest(
                "uuid-p2p-002", "P2P_TRANSFER", "SAVINGS", null, "300.00", "0.00", "P2P-002", "");

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.commissionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.ivaAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.totalDebited()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void p2pCruzadoSavingsToCheckingUsaReglaCorrecta() {
        OperationRequest req = new OperationRequest(
                "uuid-p2p-cross-001", "P2P_TRANSFER", "SAVINGS", "CHECKING", "400.00", "3.00", "P2P-X-001", "");

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.commissionAmount()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(resp.ivaAmount()).isEqualByComparingTo(new BigDecimal("0.45"));
        assertThat(resp.totalDebited()).isEqualByComparingTo(new BigDecimal("403.45"));
    }

    @Test
    void p2pCruzadoCheckingToSavingsUsaReglaCorrecta() {
        OperationRequest req = new OperationRequest(
                "uuid-p2p-cross-002", "P2P_TRANSFER", "CHECKING", "SAVINGS", "250.00", "2.00", "P2P-X-002", "");

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.commissionAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    void p2pDestinationIgualASourceTrataComoMismoTipo() {
        OperationRequest req = new OperationRequest(
                "uuid-p2p-same-003", "P2P_TRANSFER", "SAVINGS", "SAVINGS", "100.00", null, "P2P-S-003", "");

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
    }

    @Test
    void operacionDesconocidaLanzaIllegalArgument() {
        OperationRequest req = new OperationRequest(
                "uuid-bad-001", "OPERACION_INEXISTENTE", null, null, "100.00", null, "REF", null);

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(AccountingValidationException.class)
                .hasMessageContaining("Sin regla contable");
    }

    @Test
    void amountVacioLanzaIllegalArgument() {
        OperationRequest req = new OperationRequest(
                "uuid-noamt-001", "TELLER_DEPOSIT", null, null, "", null, "REF", null);

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(AccountingValidationException.class);
    }

    @Test
    void uuidVacioLanzaIllegalArgument() {
        OperationRequest req = new OperationRequest(
                "", "TELLER_DEPOSIT", null, null, "100.00", null, "REF", null);

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(AccountingValidationException.class);
    }

    @Test
    void operationTypeVacioLanzaIllegalArgument() {
        OperationRequest req = new OperationRequest(
                "uuid-notype-001", "", null, null, "100.00", null, "REF", null);

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(AccountingValidationException.class);
    }

    @Test
    void externalTransferSavingsContabilizaContraTransferenciasPorLiquidarSinTocarBovedaNiNostro() {
        CorrespondentBank pichincha = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        BigDecimal bovedaAntes = accountRepository.findById("1.1.0.02").orElseThrow().getCurrentBalance();
        BigDecimal porLiquidarAntes = accountRepository.findById("2.4.0.01").orElseThrow().getCurrentBalance();

        OperationRequest req = new OperationRequest(
                "uuid-ext-tpl-001", "EXTERNAL_TRANSFER", "SAVINGS", null, "100.00", null, "EXT-TPL-001", null, null, "002");
        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");

        // La pata del cliente convierte pasivo por pasivo: acredita la cuenta puente...
        AccountingAccount porLiquidar = accountRepository.findById("2.4.0.01").orElseThrow();
        assertThat(porLiquidar.getCurrentBalance())
                .isEqualByComparingTo(porLiquidarAntes.add(new BigDecimal("100.00")));

        // ...y no mueve ni Bóveda ni el Nostro del banco indicado.
        AccountingAccount boveda = accountRepository.findById("1.1.0.02").orElseThrow();
        assertThat(boveda.getCurrentBalance()).isEqualByComparingTo(bovedaAntes);
        AccountingAccount nostro = accountRepository.findById(pichincha.getNostroAccountCode()).orElseThrow();
        assertThat(nostro.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void externalTransferConCounterpartyBankCodeNullNoFalla() {
        OperationRequest req = new OperationRequest(
                "uuid-ext-null-001", "EXTERNAL_TRANSFER", "SAVINGS", null, "100.00", null, "EXT-NULL-001", null, null, null);

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.totalDebited()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void nostroSettlementOutboundConDosBancosDistintosContabilizaContraCuentasDetalleDistintas() {
        CorrespondentBank pichincha = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        CorrespondentBank guayaquil = provisioningService.provisionCorrespondentBank("010", "Banco de Guayaquil");

        rulesService.postOperation(new OperationRequest(
                "uuid-nout-002", "NOSTRO_SETTLEMENT_OUTBOUND", null, null, "100.00", null, "NOUT-002", null, null, "002"));
        rulesService.postOperation(new OperationRequest(
                "uuid-nout-010", "NOSTRO_SETTLEMENT_OUTBOUND", null, null, "100.00", null, "NOUT-010", null, null, "010"));

        AccountingAccount nostroPichincha = accountRepository.findById(pichincha.getNostroAccountCode()).orElseThrow();
        AccountingAccount nostroGuayaquil = accountRepository.findById(guayaquil.getNostroAccountCode()).orElseThrow();

        // La liquidación bruta DEBITA el Nostro de cada banco: saldo positivo y por banco.
        assertThat(nostroPichincha.getAccountCode()).isNotEqualTo(nostroGuayaquil.getAccountCode());
        assertThat(nostroPichincha.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(nostroGuayaquil.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void secuenciaSalienteCompletaConvierteActivoPorActivoYPasivoPorPasivo() {
        CorrespondentBank pichincha = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        BigDecimal ahorrosAntes = accountRepository.findById("2.1.0.01").orElseThrow().getCurrentBalance();
        BigDecimal bovedaAntes = accountRepository.findById("1.1.0.02").orElseThrow().getCurrentBalance();
        BigDecimal porLiquidarAntes = accountRepository.findById("2.4.0.01").orElseThrow().getCurrentBalance();

        rulesService.postOperation(new OperationRequest(
                "uuid-seq-ext-001", "EXTERNAL_TRANSFER", "SAVINGS", null, "100.00", null, "SEQ-EXT-001", null, null, "002"));
        rulesService.postOperation(new OperationRequest(
                "uuid-seq-nout-001", "NOSTRO_SETTLEMENT_OUTBOUND", null, null, "100.00", null, "SEQ-NOUT-001", null, null, "002"));

        // Pasivo por pasivo: el depósito del cliente pasa a obligación con la cámara.
        AccountingAccount ahorros = accountRepository.findById("2.1.0.01").orElseThrow();
        assertThat(ahorros.getCurrentBalance())
                .isEqualByComparingTo(ahorrosAntes.subtract(new BigDecimal("100.00")));
        AccountingAccount porLiquidar = accountRepository.findById("2.4.0.01").orElseThrow();
        assertThat(porLiquidar.getCurrentBalance())
                .isEqualByComparingTo(porLiquidarAntes.add(new BigDecimal("100.00")));

        // Activo por activo: efectivo de Bóveda pasa a posición Nostro en el corresponsal.
        AccountingAccount nostro = accountRepository.findById(pichincha.getNostroAccountCode()).orElseThrow();
        assertThat(nostro.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        AccountingAccount boveda = accountRepository.findById("1.1.0.02").orElseThrow();
        assertThat(boveda.getCurrentBalance())
                .isEqualByComparingTo(bovedaAntes.subtract(new BigDecimal("100.00")));
    }

    @Test
    void transferenciasPorLiquidarAcumulaSoloLosMontosPrincipales() {
        provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        provisioningService.provisionCorrespondentBank("010", "Banco de Guayaquil");
        BigDecimal porLiquidarAntes = accountRepository.findById("2.4.0.01").orElseThrow().getCurrentBalance();

        // Con comisión (y su IVA) en dos de las tres: esos componentes van a sus
        // propias cuentas (COMISIONES / IVA), no a la cuenta puente.
        rulesService.postOperation(new OperationRequest(
                "uuid-tpl-n-001", "EXTERNAL_TRANSFER", "SAVINGS", null, "100.00", "5.00", "TPL-N-001", null, null, "002"));
        rulesService.postOperation(new OperationRequest(
                "uuid-tpl-n-002", "EXTERNAL_TRANSFER", "CHECKING", null, "250.50", "2.00", "TPL-N-002", null, null, "010"));
        rulesService.postOperation(new OperationRequest(
                "uuid-tpl-n-003", "EXTERNAL_TRANSFER", "SAVINGS", null, "49.50", null, "TPL-N-003", null, null, "002"));

        AccountingAccount porLiquidar = accountRepository.findById("2.4.0.01").orElseThrow();
        assertThat(porLiquidar.getCurrentBalance())
                .isEqualByComparingTo(porLiquidarAntes.add(new BigDecimal("400.00")));

        // El balance estructural sigue cuadrando con la cuenta puente poblada.
        assertThat(accountingService.structuralTrialBalance(null).balanced()).isTrue();
    }

    @Test
    void nostroSettlementOutboundConBancoInexistenteLanzaExcepcionSinAsientoParcial() {
        OperationRequest req = new OperationRequest(
                "uuid-nout-bad-001", "NOSTRO_SETTLEMENT_OUTBOUND", null, null, "100.00", null, "NOUT-BAD", null, null, "999");

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(AccountingValidationException.class)
                .hasMessageContaining("999");
    }

    @Test
    void nostroSettlementOutboundConBancoInactivoLanzaExcepcionSinAsientoParcial() {
        CorrespondentBank bank = provisioningService.provisionCorrespondentBank("017", "Produbanco");
        bank.setStatus(CorrespondentBankStatus.INACTIVO);
        correspondentBankRepository.save(bank);

        OperationRequest req = new OperationRequest(
                "uuid-nout-inact-001", "NOSTRO_SETTLEMENT_OUTBOUND", null, null, "100.00", null, "NOUT-INACT", null, null, "017");

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(AccountingValidationException.class)
                .hasMessageContaining("INACTIVO");

        AccountingAccount nostro = accountRepository.findById(bank.getNostroAccountCode()).orElseThrow();
        assertThat(nostro.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void reglaSinPlaceholderSigueFuncionandoConCounterpartyBankCodeNull() {
        OperationRequest req = new OperationRequest(
                "uuid-td-null-bank-001", "TELLER_DEPOSIT", "SAVINGS", null, "500.00", "", "DEP-NB-001", "", null, null);

        PostOperationResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.totalDebited()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void vostroInboundDejaSaldoPositivoYDeliverySavingsLoRegresaACero() {
        CorrespondentBank bank = provisioningService.provisionCorrespondentBank("030", "Banco Internacional");

        OperationRequest inbound = new OperationRequest(
                "uuid-vostro-in-001", "VOSTRO_SETTLEMENT_INBOUND", null, null, "700.00", null, "VIN-001", null, null, "030");
        rulesService.postOperation(inbound);

        AccountingAccount vostroAfterInbound = accountRepository.findById(bank.getVostroAccountCode()).orElseThrow();
        assertThat(vostroAfterInbound.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("700.00"));

        OperationRequest delivery = new OperationRequest(
                "uuid-vostro-del-001", "VOSTRO_SETTLEMENT_DELIVERY", "SAVINGS", null, "700.00", null, "VDEL-001", null, null, "030");
        rulesService.postOperation(delivery);

        AccountingAccount vostroAfterDelivery = accountRepository.findById(bank.getVostroAccountCode()).orElseThrow();
        assertThat(vostroAfterDelivery.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void vostroInboundSinDeliveryQuedaEnSaldoPositivoComoEstadoValido() {
        CorrespondentBank bank = provisioningService.provisionCorrespondentBank("034", "Banco del Austro");

        OperationRequest inbound = new OperationRequest(
                "uuid-vostro-in-002", "VOSTRO_SETTLEMENT_INBOUND", null, null, "250.00", null, "VIN-002", null, null, "034");
        PostOperationResponse resp = rulesService.postOperation(inbound);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        AccountingAccount vostro = accountRepository.findById(bank.getVostroAccountCode()).orElseThrow();
        assertThat(vostro.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("250.00"));
    }
}
