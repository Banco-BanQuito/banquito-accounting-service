package ec.edu.espe.banquito.accountservice.config;

import static ec.edu.espe.banquito.accountservice.enums.AmountComponent.COMMISSION;
import static ec.edu.espe.banquito.accountservice.enums.AmountComponent.IVA_ON_COMMISSION;
import static ec.edu.espe.banquito.accountservice.enums.AmountComponent.PRINCIPAL;
import static ec.edu.espe.banquito.accountservice.enums.MovementType.CREDITO;
import static ec.edu.espe.banquito.accountservice.enums.MovementType.DEBITO;

import ec.edu.espe.banquito.accountservice.enums.AmountComponent;
import ec.edu.espe.banquito.accountservice.enums.MovementType;
import ec.edu.espe.banquito.accountservice.model.AccountingRule;
import ec.edu.espe.banquito.accountservice.model.AccountingRuleLine;
import ec.edu.espe.banquito.accountservice.repository.AccountingRuleRepository;
import ec.edu.espe.banquito.accountservice.service.ParameterService;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S1313") // códigos del plan de cuentas, no direcciones IP
@Component
@Profile("!test")
public class AccountingRulesInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountingRulesInitializer.class);
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, Month.JANUARY, 1);

    private static final String BOVEDA        = "1.1.0.02";
    private static final String BANCO_CENTRAL = "1.1.0.01";
    private static final String AHORROS       = "2.1.0.01";
    private static final String CORRIENTES    = "2.1.0.02";
    private static final String IVA_POR_PAGAR = "2.2.0.01";
    private static final String COMISIONES    = "4.1.0.01";

    private final AccountingRuleRepository ruleRepository;
    private final ParameterService parameterService;

    public AccountingRulesInitializer(AccountingRuleRepository ruleRepository,
                                      ParameterService parameterService) {
        this.ruleRepository = ruleRepository;
        this.parameterService = parameterService;
    }

    @Override
    public void run(String... args) {
        if (ruleRepository.count() > 0) {
            log.info("Reglas contables ya cargadas; se omite el seed.");
            return;
        }
        parameterService.setParameter(ParameterService.IVA_RATE, "0.15");

        List<AccountingRule> rules = List.of(
            // TELLER_DEPOSIT — ventanilla acredita cuenta cliente
            buildRule("TELLER_DEPOSIT_SAVINGS", "Depósito ventanilla → cuenta de ahorros",
                line(BOVEDA,   DEBITO,  PRINCIPAL),
                line(AHORROS,  CREDITO, PRINCIPAL)
            ),
            buildRule("TELLER_DEPOSIT_CHECKING", "Depósito ventanilla → cuenta corriente",
                line(BOVEDA,     DEBITO,  PRINCIPAL),
                line(CORRIENTES, CREDITO, PRINCIPAL)
            ),

            // TELLER_WITHDRAWAL — ventanilla debita cuenta cliente
            buildRule("TELLER_WITHDRAWAL_SAVINGS", "Retiro ventanilla ← cuenta de ahorros",
                line(AHORROS, DEBITO,  PRINCIPAL),
                line(BOVEDA,  CREDITO, PRINCIPAL)
            ),
            buildRule("TELLER_WITHDRAWAL_CHECKING", "Retiro ventanilla ← cuenta corriente",
                line(CORRIENTES, DEBITO,  PRINCIPAL),
                line(BOVEDA,     CREDITO, PRINCIPAL)
            ),

            // P2P_TRANSFER — mismo tipo de cuenta
            buildRule("P2P_TRANSFER_SAVINGS", "Transferencia P2P entre cuentas de ahorros",
                line(AHORROS,     DEBITO,  PRINCIPAL),
                line(AHORROS,     CREDITO, PRINCIPAL),
                line(AHORROS,     DEBITO,  COMMISSION),
                line(COMISIONES,  CREDITO, COMMISSION),
                line(AHORROS,     DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),
            buildRule("P2P_TRANSFER_CHECKING", "Transferencia P2P entre cuentas corrientes",
                line(CORRIENTES,  DEBITO,  PRINCIPAL),
                line(CORRIENTES,  CREDITO, PRINCIPAL),
                line(CORRIENTES,  DEBITO,  COMMISSION),
                line(COMISIONES,  CREDITO, COMMISSION),
                line(CORRIENTES,  DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),

            // P2P_TRANSFER — cruce de tipos: comisión siempre sobre cuenta origen
            buildRule("P2P_TRANSFER_SAVINGS_TO_CHECKING", "Transferencia P2P ahorros → corriente",
                line(AHORROS,     DEBITO,  PRINCIPAL),
                line(CORRIENTES,  CREDITO, PRINCIPAL),
                line(AHORROS,     DEBITO,  COMMISSION),
                line(COMISIONES,  CREDITO, COMMISSION),
                line(AHORROS,     DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),
            buildRule("P2P_TRANSFER_CHECKING_TO_SAVINGS", "Transferencia P2P corriente → ahorros",
                line(CORRIENTES,  DEBITO,  PRINCIPAL),
                line(AHORROS,     CREDITO, PRINCIPAL),
                line(CORRIENTES,  DEBITO,  COMMISSION),
                line(COMISIONES,  CREDITO, COMMISSION),
                line(CORRIENTES,  DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),

            // BATCH_CREDIT — acreditación masiva (nómina / pagos corporativos)
            buildRule("BATCH_CREDIT_SAVINGS", "Acreditación masiva → cuenta de ahorros",
                line(BANCO_CENTRAL, DEBITO,  PRINCIPAL),
                line(AHORROS,       CREDITO, PRINCIPAL),
                line(AHORROS,       DEBITO,  COMMISSION),
                line(COMISIONES,    CREDITO, COMMISSION),
                line(AHORROS,       DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),
            buildRule("BATCH_CREDIT_CHECKING", "Acreditación masiva → cuenta corriente",
                line(BANCO_CENTRAL, DEBITO,  PRINCIPAL),
                line(CORRIENTES,    CREDITO, PRINCIPAL),
                line(CORRIENTES,    DEBITO,  COMMISSION),
                line(COMISIONES,    CREDITO, COMMISSION),
                line(CORRIENTES,    DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),

            // CORPORATE_DEBIT — débito corporativo siempre desde cuenta corriente
            buildRule("CORPORATE_DEBIT_CHECKING", "Débito corporativo ← cuenta corriente",
                line(CORRIENTES,    DEBITO,  PRINCIPAL),
                line(BANCO_CENTRAL, CREDITO, PRINCIPAL),
                line(CORRIENTES,    DEBITO,  COMMISSION),
                line(COMISIONES,    CREDITO, COMMISSION),
                line(CORRIENTES,    DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            )
        );

        ruleRepository.saveAll(rules);
        log.info("Reglas contables sembradas: {} reglas para los tipos {}.",
                rules.size(),
                rules.stream().map(AccountingRule::getOperationType).toList());
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

    private AccountingRuleLine line(String accountCode, MovementType movType,
                                    AmountComponent component) {
        AccountingRuleLine l = new AccountingRuleLine();
        l.setAccountCode(accountCode);
        l.setMovementType(movType);
        l.setAmountComponent(component);
        l.setSkipIfZero(true);
        return l;
    }
}
