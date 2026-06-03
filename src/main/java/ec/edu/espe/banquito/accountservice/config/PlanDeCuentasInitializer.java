package ec.edu.espe.banquito.accountservice.config;

import ec.edu.espe.banquito.accountservice.domain.AccountType;
import ec.edu.espe.banquito.accountservice.domain.AccountingAccount;
import ec.edu.espe.banquito.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.accountservice.service.ParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Siembra el Plan de Cuentas y la fecha contable inicial SOLO si la BD está vacía.
 * Es idempotente: si la infraestructura (banquito-infra) ya cargó las cuentas,
 * este inicializador no hace nada.
 * <p>
 * Nota: GUIA_BD.html documenta 7 de las 13 cuentas. Esas 7 no cuadran por sí
 * solas (los activos no tienen contrapartida). Para que el Balance de
 * Comprobación cuadre de arranque se agrega una cuenta de PATRIMONIO/Capital
 * (código 3.1.0.01) que compensa los activos iniciales. <strong>ASUNCIÓN</strong>:
 * reemplazar este seed por las 13 cuentas reales cuando banquito-infra las publique.
 */
@Component
public class PlanDeCuentasInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanDeCuentasInitializer.class);
    private static final LocalDate FECHA_CONTABLE_INICIAL = LocalDate.of(2026, 5, 30);

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

        List<AccountingAccount> plan = List.of(
                new AccountingAccount("1.0.0.00", "ACTIVOS", AccountType.ESTRUCTURAL, BigDecimal.ZERO),
                new AccountingAccount("1.1.0.01", "Banco Central / Cámara", AccountType.DETALLE, new BigDecimal("500000.00")),
                new AccountingAccount("1.1.0.02", "Bóveda Central", AccountType.DETALLE, new BigDecimal("1000000.00")),
                new AccountingAccount("2.1.0.01", "Cuentas Ahorros Clientes", AccountType.DETALLE, BigDecimal.ZERO),
                new AccountingAccount("2.1.0.02", "Cuentas Corrientes Clientes", AccountType.DETALLE, BigDecimal.ZERO),
                new AccountingAccount("2.2.0.01", "IVA Retenido por Servicios", AccountType.DETALLE, BigDecimal.ZERO),
                new AccountingAccount("4.1.0.01", "Comisiones Pagos Masivos", AccountType.DETALLE, BigDecimal.ZERO),
                // ASUNCIÓN (no documentada en la guía): contrapartida de patrimonio que
                // balancea los activos de apertura. Saldo acreedor => negativo (débito-positivo).
                new AccountingAccount("3.1.0.01", "Patrimonio / Capital Inicial", AccountType.DETALLE, new BigDecimal("-1500000.00")));

        accountRepository.saveAll(plan);
        parameterService.setActiveContableDate(FECHA_CONTABLE_INICIAL);
        log.info("Plan de Cuentas sembrado: {} cuentas. FECHA_CONTABLE_ACTIVA={}", plan.size(), FECHA_CONTABLE_INICIAL);
    }
}
