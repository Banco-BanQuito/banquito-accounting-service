package ec.edu.espe.banquito.accountservice.config;

import ec.edu.espe.banquito.accountservice.enums.AccountType;
import ec.edu.espe.banquito.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.accountservice.service.ParameterService;
import java.io.BufferedReader;
import java.time.Month;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PlanDeCuentasInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanDeCuentasInitializer.class);
    private static final String PLAN_RESOURCE = "plan-de-cuentas.csv";
    private static final LocalDate FECHA_CONTABLE_INICIAL = LocalDate.of(2026, Month.MAY, 30);

    private final AccountingAccountRepository accountRepository;
    private final ParameterService parameterService;

    public PlanDeCuentasInitializer(AccountingAccountRepository accountRepository,
                                    ParameterService parameterService) {
        this.accountRepository = accountRepository;
        this.parameterService = parameterService;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("Plan de Cuentas ya cargado ({} cuentas); se omite el seed.", accountRepository.count());
            return;
        }
        List<AccountingAccount> plan = readPlanDeCuentas();
        accountRepository.saveAll(plan);
        parameterService.setActiveContableDate(FECHA_CONTABLE_INICIAL);
        log.info("Plan de Cuentas sembrado desde {}: {} cuentas. FECHA_CONTABLE_ACTIVA={}",
                PLAN_RESOURCE, plan.size(), FECHA_CONTABLE_INICIAL);
    }

    private List<AccountingAccount> readPlanDeCuentas() {
        List<AccountingAccount> accounts = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(PLAN_RESOURCE);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("code;")) {
                    continue;
                }
                accounts.add(parseLine(trimmed, lineNumber));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el Plan de Cuentas (" + PLAN_RESOURCE + ")", e);
        }
        return accounts;
    }

    private AccountingAccount parseLine(String line, int lineNumber) {
        String[] f = line.split(";", -1);
        if (f.length != 6) {
            throw new IllegalStateException(
                    "Linea " + lineNumber + " del Plan de Cuentas mal formada (se esperaban 6 campos): " + line);
        }
        String code         = f[0].strip();
        String name         = f[1].strip();
        AccountType type    = AccountType.valueOf(f[2].strip().toUpperCase());
        String accountClass = f[3].strip().toUpperCase();
        String parent       = f[4].strip();
        BigDecimal opening  = new BigDecimal(f[5].strip());

        boolean isAcreedora = "PASIVO".equals(accountClass) || "INGRESO".equals(accountClass);
        BigDecimal signedBalance = isAcreedora ? opening.negate() : opening;

        AccountingAccount account = new AccountingAccount();
        account.setAccountCode(code);
        account.setName(name);
        account.setAccountClass(accountClass);
        account.setAccountType(type);
        account.setParentAccountCode(parent.isBlank() ? null : parent);
        account.setCurrentBalance(signedBalance);
        return account;
    }
}
