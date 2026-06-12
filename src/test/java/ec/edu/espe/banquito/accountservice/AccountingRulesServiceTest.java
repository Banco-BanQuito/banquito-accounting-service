package ec.edu.espe.banquito.accountservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.accountservice.service.AccountingRulesService;
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
                "uuid-rd-001", "TELLER_DEPOSIT", "SAVINGS", "500.00", "", "DEP-001", "");

        JournalEntryResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
        assertThat(resp.validationResult()).isEqualTo("SUMA_CERO_OK");
        assertThat(resp.entryId()).isNotNull();
    }

    @Test
    void tellerWithdrawalRegistraAsientoBalanceado() {
        OperationRequest req = new OperationRequest(
                "uuid-rw-001", "TELLER_WITHDRAWAL", null, "200.00", null, "WIT-001", null);

        JournalEntryResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
    }

    @Test
    void postOperationEsIdempotentePorUuid() {
        OperationRequest req = new OperationRequest(
                "uuid-rd-idem-001", "TELLER_DEPOSIT", null, "300.00", null, "REF", null);

        JournalEntryResponse first = rulesService.postOperation(req);
        JournalEntryResponse second = rulesService.postOperation(req);

        assertThat(second.entryId()).isEqualTo(first.entryId());
    }

    @Test
    void p2pConComisionRegistraTodasLasLineas() {
        OperationRequest req = new OperationRequest(
                "uuid-p2p-001", "P2P_TRANSFER", "SAVINGS", "300.00", "5.00", "P2P-001", "");

        JournalEntryResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
    }

    @Test
    void p2pSinComisionOmiteLineasComisionYSigueCuadrando() {
        OperationRequest req = new OperationRequest(
                "uuid-p2p-002", "P2P_TRANSFER", "SAVINGS", "300.00", "0.00", "P2P-002", "");

        JournalEntryResponse resp = rulesService.postOperation(req);

        assertThat(resp.status()).isEqualTo("REGISTRADO");
    }

    @Test
    void operacionDesconocidaLanzaIllegalArgument() {
        OperationRequest req = new OperationRequest(
                "uuid-bad-001", "OPERACION_INEXISTENTE", null, "100.00", null, "REF", null);

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sin regla contable");
    }

    @Test
    void amountVacioLanzaIllegalArgument() {
        OperationRequest req = new OperationRequest(
                "uuid-noamt-001", "TELLER_DEPOSIT", null, "", null, "REF", null);

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uuidVacioLanzaIllegalArgument() {
        OperationRequest req = new OperationRequest(
                "", "TELLER_DEPOSIT", null, "100.00", null, "REF", null);

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void operationTypVacioLanzaIllegalArgument() {
        OperationRequest req = new OperationRequest(
                "uuid-notype-001", "", null, "100.00", null, "REF", null);

        assertThatThrownBy(() -> rulesService.postOperation(req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
