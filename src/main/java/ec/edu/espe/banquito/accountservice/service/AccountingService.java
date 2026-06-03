package ec.edu.espe.banquito.accountservice.service;

import ec.edu.espe.banquito.accountservice.domain.AccountingAccount;
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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lógica del libro mayor: registro de asientos y Balance de Comprobación. */
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

    /**
     * Registra un asiento contable validando la partida doble.
     * Idempotente por {@code entryUuid}: si ya existe, devuelve el asiento previo.
     */
    @Transactional
    public JournalEntryResponse registerEntry(JournalEntryRequest request) {
        validateRequest(request);

        // Idempotencia: un mismo entryUuid no se registra dos veces.
        var existing = journalEntryRepository.findByEntryUuid(request.entryUuid());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        validateBalanced(request.lines());

        LocalDate entryDate = request.entryDate() != null
                ? request.entryDate()
                : parameterService.getActiveContableDate();

        JournalEntry entry = new JournalEntry(request.entryUuid(), request.description(), entryDate);
        for (JournalEntryLineRequest lineReq : request.lines()) {
            AccountingAccount account = resolveDetailAccount(lineReq.accountCode());
            account.applyMovement(lineReq.movementType(), lineReq.amount());
            entry.addLine(new JournalEntryLine(account, lineReq.movementType(), lineReq.amount(), lineReq.reference()));
        }

        JournalEntry saved = journalEntryRepository.save(entry);
        return toResponse(saved);
    }

    /** Balance de Comprobación de la fecha indicada (o la fecha contable activa). */
    @Transactional(readOnly = true)
    public TrialBalanceResponse trialBalance(LocalDate date) {
        LocalDate contableDate = date != null ? date : parameterService.getActiveContableDate();

        List<TrialBalanceAccountDto> accounts = accountRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toTrialBalanceRow)
                .toList();

        BigDecimal totalDebits = accounts.stream()
                .map(TrialBalanceAccountDto::debitBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = accounts.stream()
                .map(TrialBalanceAccountDto::creditBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean balanced = totalDebits.compareTo(totalCredits) == 0;
        return new TrialBalanceResponse(contableDate, accounts, totalDebits, totalCredits, balanced);
    }

    private TrialBalanceAccountDto toTrialBalanceRow(AccountingAccount account) {
        BigDecimal balance = account.getBalance();
        BigDecimal debit = balance.signum() >= 0 ? balance : BigDecimal.ZERO;
        BigDecimal credit = balance.signum() < 0 ? balance.negate() : BigDecimal.ZERO;
        return new TrialBalanceAccountDto(account.getCode(), account.getName(), debit, credit);
    }

    private AccountingAccount resolveDetailAccount(String code) {
        AccountingAccount account = accountRepository.findByCode(code)
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
                entry.getCreatedAt());
    }
}
