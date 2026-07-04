package ec.edu.espe.banquito.core.accountservice;

import static ec.edu.espe.banquito.core.accountservice.enums.AmountComponent.COMMISSION;
import static ec.edu.espe.banquito.core.accountservice.enums.AmountComponent.IVA_ON_COMMISSION;
import static ec.edu.espe.banquito.core.accountservice.enums.AmountComponent.PRINCIPAL;
import static ec.edu.espe.banquito.core.accountservice.enums.MovementType.CREDITO;
import static ec.edu.espe.banquito.core.accountservice.enums.MovementType.DEBITO;

import ec.edu.espe.banquito.core.accountservice.enums.AccountType;
import ec.edu.espe.banquito.core.accountservice.enums.AmountComponent;
import ec.edu.espe.banquito.core.accountservice.enums.MovementType;
import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.core.accountservice.model.AccountingRule;
import ec.edu.espe.banquito.core.accountservice.model.AccountingRuleLine;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingRuleRepository;
import ec.edu.espe.banquito.core.accountservice.service.ParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestDataInitializer implements CommandLineRunner {

    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, Month.JANUARY, 1);

    private final AccountingAccountRepository accountRepository;
    private final AccountingRuleRepository ruleRepository;
    private final ParameterService parameterService;

    public TestDataInitializer(AccountingAccountRepository accountRepository,
                               AccountingRuleRepository ruleRepository,
                               ParameterService parameterService) {
        this.accountRepository = accountRepository;
        this.ruleRepository = ruleRepository;
        this.parameterService = parameterService;
    }

    @Override
    public void run(String... args) {
        accountRepository.saveAll(List.of(
                account("1.0.0.00", "Activo",                     AccountType.ESTRUCTURAL, "ACTIVO",  null,        BigDecimal.ZERO),
                account("1.1.0.01", "Banco Central",              AccountType.DETALLE,     "ACTIVO",  "1.0.0.00",  BigDecimal.ZERO),
                account("1.1.0.02", "Caja Bóveda",                AccountType.DETALLE,     "ACTIVO",  "1.0.0.00",  BigDecimal.ZERO),
                account("2.1.0.01", "Cuentas de Ahorros",         AccountType.DETALLE,     "PASIVO",  null,        BigDecimal.ZERO),
                account("2.1.0.02", "Cuentas Corrientes",         AccountType.DETALLE,     "PASIVO",  null,        BigDecimal.ZERO),
                account("2.2.0.01", "IVA Retenido por Servicios", AccountType.DETALLE,     "PASIVO",  null,        BigDecimal.ZERO),
                account("4.1.0.01", "Comisiones por Servicios",   AccountType.DETALLE,     "INGRESO", null,        BigDecimal.ZERO)
        ));

        parameterService.setActiveContableDate(LocalDate.of(2026, Month.MAY, 30));
        parameterService.setParameter(ParameterService.IVA_RATE, "0.15");

        ruleRepository.saveAll(List.of(
                buildRule("TELLER_DEPOSIT_SAVINGS", "Depósito ventanilla → ahorros",
                        line("1.1.0.02", DEBITO,  PRINCIPAL),
                        line("2.1.0.01", CREDITO, PRINCIPAL)
                ),
                buildRule("TELLER_WITHDRAWAL_SAVINGS", "Retiro ventanilla ← ahorros",
                        line("2.1.0.01", DEBITO,  PRINCIPAL),
                        line("1.1.0.02", CREDITO, PRINCIPAL)
                ),
                buildRule("P2P_TRANSFER_SAVINGS", "Transferencia P2P entre ahorros",
                        line("2.1.0.01", DEBITO,  PRINCIPAL),
                        line("2.1.0.01", CREDITO, PRINCIPAL),
                        line("2.1.0.01", DEBITO,  COMMISSION),
                        line("4.1.0.01", CREDITO, COMMISSION),
                        line("2.1.0.01", DEBITO,  IVA_ON_COMMISSION),
                        line("2.2.0.01", CREDITO, IVA_ON_COMMISSION)
                ),
                buildRule("P2P_TRANSFER_SAVINGS_TO_CHECKING", "Transferencia P2P ahorros → corriente",
                        line("2.1.0.01", DEBITO,  PRINCIPAL),
                        line("2.1.0.02", CREDITO, PRINCIPAL),
                        line("2.1.0.01", DEBITO,  COMMISSION),
                        line("4.1.0.01", CREDITO, COMMISSION),
                        line("2.1.0.01", DEBITO,  IVA_ON_COMMISSION),
                        line("2.2.0.01", CREDITO, IVA_ON_COMMISSION)
                ),
                buildRule("P2P_TRANSFER_CHECKING_TO_SAVINGS", "Transferencia P2P corriente → ahorros",
                        line("2.1.0.02", DEBITO,  PRINCIPAL),
                        line("2.1.0.01", CREDITO, PRINCIPAL),
                        line("2.1.0.02", DEBITO,  COMMISSION),
                        line("4.1.0.01", CREDITO, COMMISSION),
                        line("2.1.0.02", DEBITO,  IVA_ON_COMMISSION),
                        line("2.2.0.01", CREDITO, IVA_ON_COMMISSION)
                )
        ));
    }

    private AccountingRule buildRule(String opType, String desc, AccountingRuleLine... lineArr) {
        AccountingRule rule = new AccountingRule();
        rule.setOperationType(opType);
        rule.setDescription(desc);
        rule.setEffectiveFrom(EFFECTIVE_FROM);
        for (int i = 0; i < lineArr.length; i++) {
            lineArr[i].setLineOrder(i + 1);
            lineArr[i].setRule(rule);
        }
        rule.setLines(new ArrayList<>(Arrays.asList(lineArr)));
        return rule;
    }

    private AccountingRuleLine line(String accountCode, MovementType movType, AmountComponent component) {
        AccountingRuleLine l = new AccountingRuleLine();
        l.setAccountCode(accountCode);
        l.setMovementType(movType);
        l.setAmountComponent(component);
        l.setSkipIfZero(true);
        return l;
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
