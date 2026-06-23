package ec.edu.espe.banquito.accountservice.service;

import ec.edu.espe.banquito.accountservice.enums.AccountType;
import ec.edu.espe.banquito.accountservice.enums.EntryStatus;
import ec.edu.espe.banquito.accountservice.enums.MovementType;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.mapper.JournalEntryMapper;
import ec.edu.espe.banquito.accountservice.dto.TrialBalanceAccountDto;
import ec.edu.espe.banquito.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.accountservice.exception.InvalidAccountException;
import ec.edu.espe.banquito.accountservice.exception.UnbalancedEntryException;
import ec.edu.espe.banquito.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.accountservice.model.JournalEntry;
import ec.edu.espe.banquito.accountservice.model.JournalEntryLine;
import ec.edu.espe.banquito.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.accountservice.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
            return JournalEntryMapper.toResponse(existing.get());
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
            MovementType movementType = MovementType.valueOf(lineReq.movementType());
            AccountingAccount account = resolveDetailAccount(lineReq.accountCode());
            account.applyMovement(movementType, lineReq.amount());

            JournalEntryLine line = new JournalEntryLine();
            line.setAccount(account);
            line.setMovementType(movementType);
            line.setAmount(lineReq.amount());
            line.setReference(lineReq.reference());
            entry.addLine(line);
        }

        return JournalEntryMapper.toResponse(journalEntryRepository.save(entry));
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

    private static final String ADJUSTMENT_ACCOUNT_CODE = "5.1.0.02";

    @Transactional
    public TrialBalanceResponse autoBalance(LocalDate date) {
        TrialBalanceResponse current = trialBalance(date);
        BigDecimal diff = current.totalDebits().subtract(current.totalCredits());

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return current;
        }

        AccountingAccount adjustmentAccount = accountRepository.findById(ADJUSTMENT_ACCOUNT_CODE)
                .orElseThrow(() -> new InvalidAccountException(
                        "Cuenta de ajuste " + ADJUSTMENT_ACCOUNT_CODE + " no existe"));

        MovementType movementType = diff.signum() < 0 ? MovementType.DEBITO : MovementType.CREDITO;
        BigDecimal amount = diff.abs();

        adjustmentAccount.applyMovement(movementType, amount);
        accountRepository.save(adjustmentAccount);

        JournalEntry entry = new JournalEntry();
        entry.setEntryUuid(UUID.randomUUID().toString());
        entry.setDescription("Ajuste automatico de cuadre EOD");
        entry.setEntryDate(LocalDateTime.now());
        entry.setStatus(EntryStatus.REGISTRADO);

        JournalEntryLine line = new JournalEntryLine();
        line.setAccount(adjustmentAccount);
        line.setMovementType(movementType);
        line.setAmount(amount);
        line.setReference("AUTO-BALANCE");
        entry.addLine(line);

        journalEntryRepository.save(entry);

        return trialBalance(date);
    }

    @Transactional(readOnly = true)
    public TrialBalanceResponse structuralTrialBalance(LocalDate date) {
        LocalDate contableDate = date != null ? date : parameterService.getActiveContableDate();

        Map<String, BigDecimal> balanceByClass = accountRepository
                .findByAccountTypeOrderByAccountCodeAsc(AccountType.DETALLE)
                .stream()
                .collect(Collectors.groupingBy(
                        AccountingAccount::getAccountClass,
                        Collectors.reducing(BigDecimal.ZERO, AccountingAccount::getCurrentBalance, BigDecimal::add)));

        List<TrialBalanceAccountDto> accounts = accountRepository
                .findByAccountTypeOrderByAccountCodeAsc(AccountType.ESTRUCTURAL)
                .stream()
                .filter(a -> a.getParentAccountCode() == null || a.getParentAccountCode().isBlank())
                .map(a -> {
                    BigDecimal balance = balanceByClass.getOrDefault(a.getAccountClass(), BigDecimal.ZERO);
                    BigDecimal debit = balance.signum() >= 0 ? balance : BigDecimal.ZERO;
                    BigDecimal credit = balance.signum() < 0 ? balance.negate() : BigDecimal.ZERO;
                    return new TrialBalanceAccountDto(a.getAccountCode(), a.getName(), debit, credit);
                })
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
        if (!account.isDetail()) {
            throw new InvalidAccountException("La cuenta " + code + " no es de tipo DETALLE; no puede recibir asientos.");
        }
        return account;
    }

    private void validateRequest(JournalEntryRequest request) {
        if (request.entryUuid() == null || request.entryUuid().isBlank()) {
            throw new AccountingValidationException("entryUuid es obligatorio.");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new AccountingValidationException("El asiento debe tener al menos una linea.");
        }
        for (JournalEntryLineRequest line : request.lines()) {
            if (line.accountCode() == null || line.accountCode().isBlank()) {
                throw new AccountingValidationException("Cada linea debe indicar accountCode.");
            }
            if (line.movementType() == null) {
                throw new AccountingValidationException("Cada linea debe indicar movementType (DEBITO|CREDITO).");
            }
            if (line.amount() == null || line.amount().signum() <= 0) {
                throw new AccountingValidationException("El monto de cada linea debe ser mayor que cero.");
            }
        }
    }

    private void validateBalanced(List<JournalEntryLineRequest> lines) {
        BigDecimal debits = lines.stream()
                .filter(l -> MovementType.valueOf(l.movementType()) == MovementType.DEBITO)
                .map(JournalEntryLineRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = lines.stream()
                .filter(l -> MovementType.valueOf(l.movementType()) == MovementType.CREDITO)
                .map(JournalEntryLineRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debits.compareTo(credits) != 0) {
            throw new UnbalancedEntryException(
                    "El asiento no cuadra: debitos=" + debits + " creditos=" + credits + " (deben ser iguales).");
        }
    }

}
