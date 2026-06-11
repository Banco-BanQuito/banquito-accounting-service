package ec.edu.espe.banquito.accountservice;

import ec.edu.espe.banquito.accountservice.enums.AccountType;
import ec.edu.espe.banquito.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.accountservice.service.ParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestDataInitializer implements CommandLineRunner {

    private final AccountingAccountRepository accountRepository;
    private final ParameterService parameterService;

    public TestDataInitializer(AccountingAccountRepository accountRepository,
                               ParameterService parameterService) {
        this.accountRepository = accountRepository;
        this.parameterService = parameterService;
    }

    @Override
    public void run(String... args) {
        accountRepository.saveAll(List.of(
                account("1.0.0.00", "Activo",               AccountType.ESTRUCTURAL, "ACTIVO", null,        BigDecimal.ZERO),
                account("1.1.0.02", "Caja Bóveda",          AccountType.DETALLE,     "ACTIVO", "1.0.0.00",  BigDecimal.ZERO),
                account("2.1.0.01", "Depósitos a la Vista", AccountType.DETALLE,     "PASIVO", null,        BigDecimal.ZERO)
        ));
        parameterService.setActiveContableDate(LocalDate.of(2026, Month.MAY, 30));
    }

    private AccountingAccount account(String code, String name, AccountType type,
                                      String accountClass, String parent, BigDecimal balance) {
        AccountingAccount a = new AccountingAccount();
        a.setAccountCode(code);
        a.setName(name);
        a.setAccountType(type);
        a.setAccountClass(accountClass);
        a.setParentAccountCode(parent);
        a.setCurrentBalance(balance);
        return a;
    }
}
