package ec.edu.espe.banquito.core.accountservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.core.accountservice.dto.EodRequest;
import ec.edu.espe.banquito.core.accountservice.dto.EodResponse;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.core.accountservice.exception.EodNotBalancedException;
import ec.edu.espe.banquito.core.accountservice.exception.InvalidAccountException;
import ec.edu.espe.banquito.core.accountservice.exception.UnbalancedEntryException;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.core.accountservice.service.AccountingService;
import ec.edu.espe.banquito.core.accountservice.service.EndOfDayService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    void eodCierraYAvanzaLaFecha() {
        EodResponse eod = endOfDayService.runEndOfDay(new EodRequest("system", null));

        assertThat(eod.balanceCheck()).isEqualTo("CUADRADO");
        assertThat(eod.eodStatus()).isEqualTo("COMPLETADO");
        assertThat(eod.nextContableDate()).isEqualTo(eod.contableDateClosed().plusDays(1));
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
}
