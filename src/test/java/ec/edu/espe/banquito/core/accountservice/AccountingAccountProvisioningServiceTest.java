package ec.edu.espe.banquito.core.accountservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.espe.banquito.core.accountservice.enums.AccountType;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.core.accountservice.repository.CorrespondentBankRepository;
import ec.edu.espe.banquito.core.accountservice.service.AccountingAccountProvisioningService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccountingAccountProvisioningServiceTest {

    @Autowired
    private AccountingAccountProvisioningService provisioningService;

    @Autowired
    private CorrespondentBankRepository correspondentBankRepository;

    @Autowired
    private AccountingAccountRepository accountingAccountRepository;

    @Test
    void provisionaExactamenteDosCuentasDetalleConBalanceCero() {
        CorrespondentBank bank = provisioningService.provisionCorrespondentBank("017", "Produbanco");

        AccountingAccount nostro = accountingAccountRepository.findById(bank.getNostroAccountCode()).orElseThrow();
        AccountingAccount vostro = accountingAccountRepository.findById(bank.getVostroAccountCode()).orElseThrow();

        assertThat(nostro.getAccountCode()).isNotEqualTo(vostro.getAccountCode());
        assertThat(nostro.isDetail()).isTrue();
        assertThat(vostro.isDetail()).isTrue();
        assertThat(nostro.getAccountType()).isEqualTo(AccountType.DETALLE);
        assertThat(vostro.getAccountType()).isEqualTo(AccountType.DETALLE);
        assertThat(nostro.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(vostro.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nostro.getAccountClass()).isEqualTo("ACTIVO");
        assertThat(vostro.getAccountClass()).isEqualTo("PASIVO");
        assertThat(nostro.getParentAccountCode()).isEqualTo("1.1.1.00");
        assertThat(vostro.getParentAccountCode()).isEqualTo("2.3.0.00");
    }

    @Test
    void dosBancosDistintosGeneranCodigosDeCuentaUnicos() {
        CorrespondentBank first = provisioningService.provisionCorrespondentBank("030", "Banco Internacional");
        CorrespondentBank second = provisioningService.provisionCorrespondentBank("034", "Banco del Austro");

        assertThat(first.getNostroAccountCode()).isNotEqualTo(second.getNostroAccountCode());
        assertThat(first.getVostroAccountCode()).isNotEqualTo(second.getVostroAccountCode());
    }

    @Test
    void rechazaAltaSiElBancoYaExiste() {
        provisioningService.provisionCorrespondentBank("035", "Banco Bolivariano");

        assertThatThrownBy(() -> provisioningService.provisionCorrespondentBank("035", "Banco Bolivariano Duplicado"))
                .isInstanceOf(AccountingValidationException.class)
                .hasMessageContaining("035");

        assertThat(correspondentBankRepository.findById("035")).hasValueSatisfying(
                bank -> assertThat(bank.getBankName()).isEqualTo("Banco Bolivariano"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void fallaAtomicamenteSinDejarCuentasHuerfanasCuandoLaInsercionFalla() {
        // Este test corre SIN la transacción envolvente de la clase (NOT_SUPPORTED):
        // así, la @Transactional propia de provisionCorrespondentBank hace un
        // commit/rollback real e independiente, y podemos verificar el estado
        // posterior en una transacción nueva y limpia, no en una ya marcada
        // rollback-only. bankName de 300 caracteres viola accounting_account.name
        // VARCHAR(100): la cuenta Nostro falla a mitad de la transacción de alta.
        // No hay rollback automático de test aquí, así que el bankCode usado no
        // debe chocar con otros tests ni quedar huésped permanentemente si algo cambia.
        String bankNameDemasiadoLargo = "X".repeat(300);

        assertThatThrownBy(() -> provisioningService.provisionCorrespondentBank("999", bankNameDemasiadoLargo))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(correspondentBankRepository.findById("999")).isEmpty();
        assertThat(accountingAccountRepository.findAllByOrderByAccountCodeAsc()).noneMatch(
                account -> "1.1.1.00".equals(account.getParentAccountCode())
                        && account.getName().startsWith("Nostro XXX"));
    }
}
