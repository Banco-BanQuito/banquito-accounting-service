package ec.edu.espe.banquito.accountservice.service;

import ec.edu.espe.banquito.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.accountservice.enums.AmountComponent;
import ec.edu.espe.banquito.accountservice.model.AccountingRule;
import ec.edu.espe.banquito.accountservice.model.AccountingRuleLine;
import ec.edu.espe.banquito.accountservice.repository.AccountingRuleRepository;
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
    public JournalEntryResponse postOperation(OperationRequest request) {
        validateRequest(request);

        LocalDate contableDate = (request.accountingDate() == null || request.accountingDate().isBlank())
                ? parameterService.getActiveContableDate()
                : parseAccountingDate(request.accountingDate());

        AccountingRule rule = ruleRepository
                .findActiveByType(request.operationType(), contableDate)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sin regla contable para '" + request.operationType() + "' el " + contableDate));

        BigDecimal principal = new BigDecimal(request.amount());
        BigDecimal commission = (request.commissionAmount() == null || request.commissionAmount().isBlank())
                ? BigDecimal.ZERO : new BigDecimal(request.commissionAmount());
        BigDecimal ivaRate = parameterService.getIvaRate();

        List<JournalEntryLineRequest> lines = rule.getLines().stream()
                .map(l -> toLineRequest(l, principal, commission, ivaRate, request.reference()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "La operación '" + request.operationType() + "' genera asiento vacío con los montos dados.");
        }

        return accountingService.registerEntry(new JournalEntryRequest(
                request.operationUuid(),
                request.operationType() + " | " + request.reference(),
                contableDate,
                lines));
    }

    private Optional<JournalEntryLineRequest> toLineRequest(AccountingRuleLine line,
            BigDecimal principal, BigDecimal commission, BigDecimal ivaRate, String reference) {
        BigDecimal amount = switch (line.getAmountComponent()) {
            case PRINCIPAL -> principal;
            case COMMISSION -> commission;
            case IVA_ON_COMMISSION -> commission.multiply(ivaRate).setScale(2, RoundingMode.HALF_UP);
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
            throw new IllegalArgumentException("operationUuid es obligatorio.");
        }
        if (request.operationType() == null || request.operationType().isBlank()) {
            throw new IllegalArgumentException("operationType es obligatorio.");
        }
        if (request.amount() == null || request.amount().isBlank()) {
            throw new IllegalArgumentException("amount es obligatorio.");
        }
        try {
            new BigDecimal(request.amount());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("amount no es un número válido: " + request.amount());
        }
    }

    private LocalDate parseAccountingDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("accountingDate no tiene formato válido (YYYY-MM-DD): " + raw);
        }
    }
}
