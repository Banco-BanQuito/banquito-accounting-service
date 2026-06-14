package ec.edu.espe.banquito.accountservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.espe.banquito.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.accountservice.dto.PostOperationResponse;
import ec.edu.espe.banquito.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.accountservice.service.AccountingRulesService;
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
}
