package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.core.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.core.accountservice.dto.PostOperationResponse;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.model.AccountingRule;
import ec.edu.espe.banquito.core.accountservice.model.AccountingRuleLine;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingRuleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EL TRADUCTOR ENTRE EL NEGOCIO Y LA CONTABILIDAD.
 *
 * <p>El resto del banco (el módulo de transferencias, el de depósitos, etc.) no sabe de
 * contabilidad. Solo avisa cosas como: "se hizo una transferencia de $100 con $2 de
 * comisión". Esta clase es la traductora: toma ese aviso en lenguaje de negocio y lo
 * convierte en el movimiento contable correcto.</p>
 *
 * <p>¿Cómo lo hace? Tiene un "recetario" de reglas. Para cada tipo de operación existe
 * una receta que dice a qué cuentas afecta y cómo. Esta clase busca la receta adecuada,
 * calcula los montos (el monto principal, la comisión y el IVA de la comisión) y arma el
 * movimiento. El anotarlo en el libro se lo deja al {@link AccountingService}.</p>
 */
@Service
public class AccountingRulesService {

    /** El "recetario": guarda las reglas de qué cuentas usar para cada tipo de operación. */
    private final AccountingRuleRepository ruleRepository;
    /** El libro contable, a quien le pedimos que anote el movimiento ya armado. */
    private final AccountingService accountingService;
    /** Nos da la fecha contable de hoy y el porcentaje de IVA vigente. */
    private final ParameterService parameterService;

    public AccountingRulesService(AccountingRuleRepository ruleRepository,
                                  AccountingService accountingService,
                                  ParameterService parameterService) {
        this.ruleRepository = ruleRepository;
        this.accountingService = accountingService;
        this.parameterService = parameterService;
    }

    /**
     * CONTABILIZAR UNA OPERACIÓN DEL BANCO (el método principal).
     *
     * <p>Sigue estos pasos, como una receta de cocina:</p>
     * <ol>
     *   <li>Revisa que el aviso venga completo.</li>
     *   <li>Averigua sobre qué día contable se registra.</li>
     *   <li>Identifica de qué operación se trata exactamente (según los productos que
     *       intervienen, ej. transferencia entre ahorros y corriente).</li>
     *   <li>Busca en el recetario la regla que aplica.</li>
     *   <li>Calcula los montos: el principal, la comisión y el IVA de la comisión.</li>
     *   <li>Arma el movimiento y le pide al libro contable que lo anote.</li>
     * </ol>
     *
     * <p>Al final devuelve un resumen: el identificador del movimiento anotado y los
     * montos calculados (comisión, IVA y el total cobrado al cliente).</p>
     */
    @Transactional
    public PostOperationResponse postOperation(OperationRequest request) {
        validateRequest(request);

        LocalDate contableDate = (request.accountingDate() == null || request.accountingDate().isBlank())
                ? parameterService.getActiveContableDate()
                : parseAccountingDate(request.accountingDate());

        String effectiveType = buildEffectiveType(request);

        AccountingRule rule = ruleRepository
                .findActiveByType(effectiveType, contableDate)
                .stream().findFirst()
                .orElseThrow(() -> new AccountingValidationException(
                        "Sin regla contable para '" + effectiveType + "' el " + contableDate));

        BigDecimal principal = new BigDecimal(request.amount());
        BigDecimal commission = (request.commissionAmount() == null || request.commissionAmount().isBlank())
                ? BigDecimal.ZERO : new BigDecimal(request.commissionAmount());
        BigDecimal ivaAmount;
        if (request.ivaAmount() != null && !request.ivaAmount().isBlank()) {
            ivaAmount = new BigDecimal(request.ivaAmount());
        } else {
            ivaAmount = commission.multiply(parameterService.getIvaRate()).setScale(2, RoundingMode.HALF_UP);
        }

        List<JournalEntryLineRequest> lines = rule.getLines().stream()
                .map(l -> toLineRequest(l, principal, commission, ivaAmount, request.reference()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (lines.isEmpty()) {
            throw new AccountingValidationException(
                    "La operación '" + effectiveType + "' genera asiento vacío con los montos dados.");
        }

        var entry = accountingService.registerEntry(new JournalEntryRequest(
                request.operationUuid(),
                effectiveType + " | " + request.reference(),
                contableDate,
                lines));

        return new PostOperationResponse(
                entry.entryId(),
                entry.entryUuid(),
                entry.status(),
                entry.validationResult(),
                entry.registeredAt(),
                commission,
                ivaAmount,
                principal.add(commission).add(ivaAmount));
    }

    /**
     * Toma un paso de la receta y lo convierte en una línea real del movimiento. Según lo
     * que diga la receta, usa el monto principal, el de la comisión o el del IVA. Si a esa
     * línea le toca $0 y la receta indica que en ese caso se puede saltar, la omite (para
     * no ensuciar el movimiento con líneas en cero).
     */
    private Optional<JournalEntryLineRequest> toLineRequest(AccountingRuleLine line,
            BigDecimal principal, BigDecimal commission, BigDecimal ivaAmount, String reference) {
        BigDecimal amount = switch (line.getAmountComponent()) {
            case PRINCIPAL -> principal;
            case COMMISSION -> commission;
            case IVA_ON_COMMISSION -> ivaAmount;
        };
        if (line.isSkipIfZero() && amount.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        return Optional.of(new JournalEntryLineRequest(
                line.getAccountCode(),
                line.getMovementType().name(),
                amount,
                (reference == null || reference.isBlank()) ? null : reference));
    }

    /**
     * Revisa que el aviso de la operación traiga lo mínimo indispensable antes de
     * procesarlo: un código único, el tipo de operación y un monto que además sea un
     * número válido (no texto ni algo vacío).
     */
    private void validateRequest(OperationRequest request) {
        if (request.operationUuid() == null || request.operationUuid().isBlank()) {
            throw new AccountingValidationException("operationUuid es obligatorio.");
        }
        if (request.operationType() == null || request.operationType().isBlank()) {
            throw new AccountingValidationException("operationType es obligatorio.");
        }
        if (request.amount() == null || request.amount().isBlank()) {
            throw new AccountingValidationException("amount es obligatorio.");
        }
        try {
            new BigDecimal(request.amount());
        } catch (NumberFormatException e) {
            throw new AccountingValidationException("amount no es un número válido: " + request.amount());
        }
    }

    /**
     * Arma el "nombre exacto" de la operación para saber qué receta buscar. No es lo mismo
     * una transferencia genérica que una transferencia de una cuenta de ahorros a una
     * corriente: pueden llevar cuentas distintas. Por eso combina el tipo de operación con
     * los productos de origen y destino, dando nombres como {@code TRANSFERENCIA},
     * {@code TRANSFERENCIA_AHORROS} o {@code TRANSFERENCIA_AHORROS_TO_CORRIENTE}.
     */
    private String buildEffectiveType(OperationRequest request) {
        String src = request.sourceAccountProductType();
        String dst = request.destinationAccountProductType();
        boolean hasSrc = src != null && !src.isBlank();
        boolean hasDst = dst != null && !dst.isBlank();
        if (!hasSrc) {
            return request.operationType();
        }
        if (!hasDst || dst.equalsIgnoreCase(src)) {
            return request.operationType() + "_" + src;
        }
        return request.operationType() + "_" + src + "_TO_" + dst;
    }

    /** Interpreta la fecha que llega como texto (formato año-mes-día). Si viene mal escrita, la rechaza. */
    private LocalDate parseAccountingDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new AccountingValidationException("accountingDate no tiene formato válido (YYYY-MM-DD): " + raw);
        }
    }
}
