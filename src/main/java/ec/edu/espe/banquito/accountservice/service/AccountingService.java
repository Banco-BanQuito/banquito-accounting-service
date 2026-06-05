package ec.edu.espe.banquito.accountservice.service;

import ec.edu.espe.banquito.accountservice.domain.AccountingAccount;
import ec.edu.espe.banquito.accountservice.domain.EntryStatus;
import ec.edu.espe.banquito.accountservice.domain.JournalEntry;
import ec.edu.espe.banquito.accountservice.domain.JournalEntryLine;
import ec.edu.espe.banquito.accountservice.domain.MovementType;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.dto.TrialBalanceAccountDto;
import ec.edu.espe.banquito.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.accountservice.exception.InvalidAccountException;
import ec.edu.espe.banquito.accountservice.exception.UnbalancedEntryException;
import ec.edu.espe.banquito.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.accountservice.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountingService {

    private final AccountingAccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final ParameterService parameterService;

    public AccountingService(AccountingAccountRepository accountRepository,
                             JournalEntryRepository journalEntryRepository,
                             ParameterService parameterService) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.parameterService = parameterService;
    }

    @Transactional
    public JournalEntryResponse registerEntry(JournalEntryRequest request) {
        validateRequest(request);

        var existing = journalEntryRepository.findByEntryUuid(request.entryUuid());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        validateBalanced(request.lines());

        LocalDateTime entryDate = request.entryDate() != null
                ? request.entryDate().atStartOfDay()
                : parameterService.getActiveContableDate().atStartOfDay();

        JournalEntry entry = new JournalEntry();
        entry.setEntryUuid(request.entryUuid());
        entry.setDescription(request.description());
        entry.setEntryDate(entryDate);
        entry.setStatus(EntryStatus.REGISTRADO);

        for (JournalEntryLineRequest lineReq : request.lines()) {
            AccountingAccount account = resolveDetailAccount(lineReq.accountCode());
            account.applyMovement(lineReq.movementType(), lineReq.amount());

            JournalEntryLine line = new JournalEntryLine();
            line.setAccount(account);
            line.setMovementType(lineReq.movementType());
            line.setAmount(lineReq.amount());
            line.setReference(lineReq.reference());
            entry.addLine(line);
        }

        return toResponse(journalEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public TrialBalanceResponse trialBalance(LocalDate date) {
        LocalDate contableDate = date != null ? date : parameterService.getActiveContableDate();

        List<TrialBalanceAccountDto> accounts = accountRepository.findAllByOrderByAccountCodeAsc().stream()
                .map(this::toTrialBalanceRow)
                .toList();

        BigDecimal totalDebits = accounts.stream()
                .map(TrialBalanceAccountDto::debitBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = accounts.stream()
                .map(TrialBalanceAccountDto::creditBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TrialBalanceResponse(contableDate, accounts, totalDebits, totalCredits,
                totalDebits.compareTo(totalCredits) == 0);
    }

    private TrialBalanceAccountDto toTrialBalanceRow(AccountingAccount account) {
        BigDecimal balance = account.getCurrentBalance();
        BigDecimal debit = balance.signum() >= 0 ? balance : BigDecimal.ZERO;
        BigDecimal credit = balance.signum() < 0 ? balance.negate() : BigDecimal.ZERO;
        return new TrialBalanceAccountDto(account.getAccountCode(), account.getName(), debit, credit);
    }

    private AccountingAccount resolveDetailAccount(String code) {
        AccountingAccount account = accountRepository.findById(code)
                .orElseThrow(() -> new InvalidAccountException("La cuenta " + code + " no existe en el Plan de Cuentas."));
        if (!account.isDetalle()) {
            throw new InvalidAccountException("La cuenta " + code + " no es de tipo DETALLE; no puede recibir asientos.");
        }
        return account;
    }

    private void validateRequest(JournalEntryRequest request) {
        if (request.entryUuid() == null || request.entryUuid().isBlank()) {
            throw new IllegalArgumentException("entryUuid es obligatorio.");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("El asiento debe tener al menos una línea.");
        }
        for (JournalEntryLineRequest line : request.lines()) {
            if (line.accountCode() == null || line.accountCode().isBlank()) {
                throw new IllegalArgumentException("Cada línea debe indicar accountCode.");
            }
            if (line.movementType() == null) {
                throw new IllegalArgumentException("Cada línea debe indicar movementType (DEBITO|CREDITO).");
            }
            if (line.amount() == null || line.amount().signum() <= 0) {
                throw new IllegalArgumentException("El monto de cada línea debe ser mayor que cero.");
            }
        }
    }

    private void validateBalanced(List<JournalEntryLineRequest> lines) {
        BigDecimal debits = lines.stream()
                .filter(l -> l.movementType() == MovementType.DEBITO)
                .map(JournalEntryLineRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = lines.stream()
                .filter(l -> l.movementType() == MovementType.CREDITO)
                .map(JournalEntryLineRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debits.compareTo(credits) != 0) {
            throw new UnbalancedEntryException(
                    "El asiento no cuadra: débitos=" + debits + " créditos=" + credits + " (deben ser iguales).");
        }
    }

    private JournalEntryResponse toResponse(JournalEntry entry) {
        return new JournalEntryResponse(
                entry.getId(),
                entry.getEntryUuid(),
                entry.getStatus().name(),
                "SUMA_CERO_OK",
                entry.getEntryDate());
    }
}
