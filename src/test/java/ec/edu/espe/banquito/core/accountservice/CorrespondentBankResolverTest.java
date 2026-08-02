package ec.edu.espe.banquito.core.accountservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.espe.banquito.core.accountservice.enums.CorrespondentBankStatus;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;
import ec.edu.espe.banquito.core.accountservice.repository.CorrespondentBankRepository;
import ec.edu.espe.banquito.core.accountservice.service.AccountingAccountProvisioningService;
import ec.edu.espe.banquito.core.accountservice.service.CorrespondentBankResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CorrespondentBankResolverTest {

    @Autowired
    private CorrespondentBankResolver resolver;

    @Autowired
    private AccountingAccountProvisioningService provisioningService;

    @Autowired
    private CorrespondentBankRepository correspondentBankRepository;

    @Test
    void resuelveCuentasNostroYVostroParaBankCodeValido() {
        CorrespondentBank bank = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");

        assertThat(resolver.resolveNostroAccount("002")).isEqualTo(bank.getNostroAccountCode());
        assertThat(resolver.resolveVostroAccount("002")).isEqualTo(bank.getVostroAccountCode());
        assertThat(bank.getNostroAccountCode()).startsWith("1.1.1.");
        assertThat(bank.getVostroAccountCode()).startsWith("2.3.0.");
    }

    @Test
    void lanzaExcepcionSiElBancoNoExiste() {
        assertThatThrownBy(() -> resolver.resolveNostroAccount("999"))
                .isInstanceOf(AccountingValidationException.class)
                .hasMessageContaining("999");
    }

    @Test
    void lanzaExcepcionSiElBancoEstaInactivo() {
        CorrespondentBank bank = provisioningService.provisionCorrespondentBank("010", "Banco de Guayaquil");
        bank.setStatus(CorrespondentBankStatus.INACTIVO);
        correspondentBankRepository.save(bank);

        assertThatThrownBy(() -> resolver.resolveVostroAccount("010"))
                .isInstanceOf(AccountingValidationException.class)
                .hasMessageContaining("INACTIVO");
    }
}
