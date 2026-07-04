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

@Service
public class AccountingRulesService {

    private final AccountingRuleRepository ruleRepository;
    private final AccountingService accountingService;
    private final ParameterService parameterService;

    public AccountingRulesService(AccountingRuleRepository ruleRepository,
                                  AccountingService accountingService,
                                  ParameterService parameterService) {
        this.ruleRepository = ruleRepository;
        this.accountingService = accountingService;
        this.parameterService = parameterService;
    }

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

    private LocalDate parseAccountingDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new AccountingValidationException("accountingDate no tiene formato válido (YYYY-MM-DD): " + raw);
        }
    }
}
