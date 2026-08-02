package ec.edu.espe.banquito.core.accountservice.config;

import static ec.edu.espe.banquito.core.accountservice.enums.AmountComponent.COMMISSION;
import static ec.edu.espe.banquito.core.accountservice.enums.AmountComponent.IVA_ON_COMMISSION;
import static ec.edu.espe.banquito.core.accountservice.enums.AmountComponent.PRINCIPAL;
import static ec.edu.espe.banquito.core.accountservice.enums.MovementType.CREDITO;
import static ec.edu.espe.banquito.core.accountservice.enums.MovementType.DEBITO;

import ec.edu.espe.banquito.core.accountservice.enums.AmountComponent;
import ec.edu.espe.banquito.core.accountservice.enums.MovementType;
import ec.edu.espe.banquito.core.accountservice.model.AccountingRule;
import ec.edu.espe.banquito.core.accountservice.model.AccountingRuleLine;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingRuleRepository;
import ec.edu.espe.banquito.core.accountservice.service.ParameterService;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S1313")
@Component
public class AccountingRulesInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountingRulesInitializer.class);
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, Month.JANUARY, 1);

    private static final String BOVEDA        = "1.1.0.02";
    private static final String BANCO_CENTRAL = "1.1.0.01";
    private static final String AHORROS       = "2.1.0.01";
    private static final String CORRIENTES    = "2.1.0.02";
    private static final String IVA_POR_PAGAR = "2.2.0.01";
    private static final String TRANSFERENCIAS_POR_LIQUIDAR = "2.4.0.01";
    private static final String COMISIONES    = "4.1.0.01";

    /**
     * Placeholders resueltos en tiempo de ejecución por CorrespondentBankResolver
     * (AccountingRulesService.toLineRequest()), no códigos de cuenta reales.
     *
     * Semántica Nostro/Vostro (perspectiva del banco propio, BanQuito):
     * - NOSTRO ("nuestra cuenta en el banco corresponsal"): activo (naturaleza DEUDORA).
     *   Sube con DEBITO cuando enviamos fondos al corresponsal (pago saliente),
     *   baja con CREDITO cuando el corresponsal nos confirma/liquida en su contra.
     * - VOSTRO ("la cuenta del corresponsal en nuestros libros"): pasivo (naturaleza ACREEDORA).
     *   Sube con CREDITO cuando recibimos fondos brutos del corresponsal (aún no
     *   entregados al cliente final), baja con DEBITO cuando entregamos esos fondos
     *   al cliente destinatario.
     *
     * Simetría de flujos interbancarios (dos patas por transferencia):
     * - ENTRANTE: VOSTRO_SETTLEMENT_INBOUND (Bóveda DEBITO / Vostro CREDITO) recibe
     *   los fondos brutos del corresponsal; VOSTRO_SETTLEMENT_DELIVERY_* (Vostro
     *   DEBITO / cliente CREDITO) los entrega al cliente destinatario.
     * - SALIENTE: EXTERNAL_TRANSFER_* (cliente DEBITO / TRANSFERENCIAS_POR_LIQUIDAR
     *   CREDITO) es la pata del cliente que dispara account-core-service;
     *   NOSTRO_SETTLEMENT_OUTBOUND (Nostro DEBITO / Bóveda CREDITO) es la pata de
     *   liquidación bruta que disparará la cámara de compensación por transacción
     *   (Fase 5).
     *
     * En liquidación diferida el dinero NO sale del banco en la pata del cliente —
     * se CONVIERTE, siempre dentro de la misma clase contable:
     * - Pasivo por pasivo (pata cliente): el depósito del cliente (2.1.x) pasa a ser
     *   obligación con la cámara de compensación (TRANSFERENCIAS_POR_LIQUIDAR,
     *   2.4.0.01 — cuenta puente ÚNICA y global: la obligación es con la cámara /
     *   el sistema, no bilateral por banco; por eso no es dinámica).
     * - Activo por activo (pata de liquidación): efectivo de Bóveda pasa a posición
     *   Nostro en el banco corresponsal.
     *
     * Significado económico de los saldos residuales:
     * - TRANSFERENCIAS_POR_LIQUIDAR (+): monto principal enviado por clientes que la
     *   cámara aún no concilia. Nunca negativo (solo lo acredita EXTERNAL_TRANSFER_*).
     * - NOSTRO por banco (+): fondos ya posicionados en el corresponsal, pendientes
     *   de conciliación. Solo lo mueve NOSTRO_SETTLEMENT_OUTBOUND, siempre DEBITO:
     *   nunca negativo (sin pre-fondeo).
     * - Bóveda se mueve exactamente UNA vez por transferencia completa (−monto, en
     *   la pata de liquidación).
     *
     * Conciliación futura con el Banco Central (fuera de alcance hoy): un asiento
     * DEBITO TRANSFERENCIAS_POR_LIQUIDAR / CREDITO NOSTRO_DYNAMIC cerraría ambos
     * saldos residuales a la vez — la obligación con la cámara se extingue contra
     * la posición mantenida en el corresponsal.
     */
    private static final String NOSTRO_DYNAMIC = "NOSTRO_DYNAMIC";
    private static final String VOSTRO_DYNAMIC = "VOSTRO_DYNAMIC";

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
            buildRule("TELLER_DEPOSIT_SAVINGS", "Depósito ventanilla → cuenta de ahorros",
                line(BOVEDA,   DEBITO,  PRINCIPAL),
                line(AHORROS,  CREDITO, PRINCIPAL)
            ),
            buildRule("TELLER_DEPOSIT_CHECKING", "Depósito ventanilla → cuenta corriente",
                line(BOVEDA,     DEBITO,  PRINCIPAL),
                line(CORRIENTES, CREDITO, PRINCIPAL)
            ),

            buildRule("TELLER_WITHDRAWAL_SAVINGS", "Retiro ventanilla ← cuenta de ahorros",
                line(AHORROS, DEBITO,  PRINCIPAL),
                line(BOVEDA,  CREDITO, PRINCIPAL)
            ),
            buildRule("TELLER_WITHDRAWAL_CHECKING", "Retiro ventanilla ← cuenta corriente",
                line(CORRIENTES, DEBITO,  PRINCIPAL),
                line(BOVEDA,     CREDITO, PRINCIPAL)
            ),

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

            buildRule("CORPORATE_DEBIT_CHECKING", "Débito corporativo ← cuenta corriente",
                line(CORRIENTES,    DEBITO,  PRINCIPAL),
                line(BANCO_CENTRAL, CREDITO, PRINCIPAL),
                line(CORRIENTES,    DEBITO,  COMMISSION),
                line(COMISIONES,    CREDITO, COMMISSION),
                line(CORRIENTES,    DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),

            buildRule("CORPORATE_DEBIT_SAVINGS", "Débito corporativo ← cuenta de ahorros",
                line(AHORROS,       DEBITO,  PRINCIPAL),
                line(BANCO_CENTRAL, CREDITO, PRINCIPAL),
                line(AHORROS,       DEBITO,  COMMISSION),
                line(COMISIONES,    CREDITO, COMMISSION),
                line(AHORROS,       DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),

            buildRule("CORPORATE_REFUND_CHECKING", "Devolución corporativa → cuenta corriente",
                line(BANCO_CENTRAL, DEBITO,  PRINCIPAL),
                line(CORRIENTES,    CREDITO, PRINCIPAL),
                line(CORRIENTES,    DEBITO,  COMMISSION),
                line(COMISIONES,    CREDITO, COMMISSION),
                line(CORRIENTES,    DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),

            buildRule("CORPORATE_REFUND_SAVINGS", "Devolución corporativa → cuenta de ahorros",
                line(BANCO_CENTRAL, DEBITO,  PRINCIPAL),
                line(AHORROS,       CREDITO, PRINCIPAL),
                line(AHORROS,       DEBITO,  COMMISSION),
                line(COMISIONES,    CREDITO, COMMISSION),
                line(AHORROS,       DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR, CREDITO, IVA_ON_COMMISSION)
            ),

            buildRule("EXTERNAL_TRANSFER_SAVINGS", "Transferencia interbancaria ← cuenta de ahorros",
                line(AHORROS,                     DEBITO,  PRINCIPAL),
                line(TRANSFERENCIAS_POR_LIQUIDAR, CREDITO, PRINCIPAL),
                line(AHORROS,                     DEBITO,  COMMISSION),
                line(COMISIONES,                  CREDITO, COMMISSION),
                line(AHORROS,                     DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR,               CREDITO, IVA_ON_COMMISSION)
            ),
            buildRule("EXTERNAL_TRANSFER_CHECKING", "Transferencia interbancaria ← cuenta corriente",
                line(CORRIENTES,                  DEBITO,  PRINCIPAL),
                line(TRANSFERENCIAS_POR_LIQUIDAR, CREDITO, PRINCIPAL),
                line(CORRIENTES,                  DEBITO,  COMMISSION),
                line(COMISIONES,                  CREDITO, COMMISSION),
                line(CORRIENTES,                  DEBITO,  IVA_ON_COMMISSION),
                line(IVA_POR_PAGAR,               CREDITO, IVA_ON_COMMISSION)
            ),

            buildRule("OFFUS_SETTLEMENT", "Liquidación de compensación interbancaria (off-us)",
                line(BANCO_CENTRAL, DEBITO,  PRINCIPAL),
                line(BOVEDA,        CREDITO, PRINCIPAL)
            ),

            buildRule("NOSTRO_SETTLEMENT_OUTBOUND", "Liquidación bruta de pago saliente a banco corresponsal",
                line(NOSTRO_DYNAMIC, DEBITO,  PRINCIPAL),
                line(BOVEDA,         CREDITO, PRINCIPAL)
            ),

            buildRule("VOSTRO_SETTLEMENT_INBOUND", "Recepción bruta de fondos de banco corresponsal (aún no entregados al cliente)",
                line(BOVEDA,         DEBITO,  PRINCIPAL),
                line(VOSTRO_DYNAMIC, CREDITO, PRINCIPAL)
            ),

            buildRule("VOSTRO_SETTLEMENT_DELIVERY_SAVINGS", "Entrega al cliente de fondos recibidos de banco corresponsal → ahorros",
                line(VOSTRO_DYNAMIC, DEBITO,  PRINCIPAL),
                line(AHORROS,        CREDITO, PRINCIPAL)
            ),
            buildRule("VOSTRO_SETTLEMENT_DELIVERY_CHECKING", "Entrega al cliente de fondos recibidos de banco corresponsal → corriente",
                line(VOSTRO_DYNAMIC, DEBITO,  PRINCIPAL),
                line(CORRIENTES,     CREDITO, PRINCIPAL)
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
