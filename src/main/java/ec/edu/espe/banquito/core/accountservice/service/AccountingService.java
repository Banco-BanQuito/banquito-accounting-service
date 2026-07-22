package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.enums.AccountNature;
import ec.edu.espe.banquito.core.accountservice.enums.AccountType;
import ec.edu.espe.banquito.core.accountservice.enums.EntryStatus;
import ec.edu.espe.banquito.core.accountservice.enums.MovementType;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryDetailDto;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.core.accountservice.mapper.JournalEntryMapper;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceAccountDto;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.exception.EntryAlreadyReversedException;
import ec.edu.espe.banquito.core.accountservice.exception.EntryNotFoundException;
import ec.edu.espe.banquito.core.accountservice.exception.InvalidAccountException;
import ec.edu.espe.banquito.core.accountservice.exception.UnbalancedEntryException;
import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.core.accountservice.model.JournalEntry;
import ec.edu.espe.banquito.core.accountservice.model.JournalEntryLine;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.core.accountservice.repository.JournalEntryRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
        entry.setSourceAccountNumber(blankToNull(request.sourceAccountNumber()));
        entry.setDestinationAccountNumber(blankToNull(request.destinationAccountNumber()));
        entry.setBeneficiaryName(blankToNull(request.beneficiaryName()));
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
                    return toTrialBalanceRow(a.getAccountCode(), a.getName(), a.getAccountClass(), balance);
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
        return toTrialBalanceRow(account.getAccountCode(), account.getName(),
                account.getAccountClass(), account.getCurrentBalance());
    }

    private TrialBalanceAccountDto toTrialBalanceRow(String code, String name,
                                                     String accountClass, BigDecimal balance) {
        boolean esDeudora = AccountNature.fromClass(accountClass) == AccountNature.DEUDORA;
        BigDecimal debit = esDeudora ? balance : BigDecimal.ZERO;
        BigDecimal credit = esDeudora ? BigDecimal.ZERO : balance;
        return new TrialBalanceAccountDto(code, name, debit, credit);
    }

    private AccountingAccount resolveDetailAccount(String code) {
        AccountingAccount account = accountRepository.findByIdForUpdate(code)
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

    @Transactional
    public JournalEntryDetailDto reverseEntry(String entryUuid) {
        JournalEntry original = findEntryOrThrow(entryUuid);

        if (original.getStatus() == EntryStatus.ANULADO) {
            throw new EntryAlreadyReversedException("El asiento " + entryUuid + " ya fue reversado.");
        }
        if (journalEntryRepository.findByReversalOfEntry_Id(original.getId()).isPresent()) {
            throw new EntryAlreadyReversedException("El asiento " + entryUuid + " ya tiene un reverso registrado.");
        }

        JournalEntry reversal = new JournalEntry();
        reversal.setEntryUuid(UUID.randomUUID().toString());
        reversal.setDescription("Reverso de: " + original.getDescription());
        reversal.setEntryDate(LocalDateTime.now());
        reversal.setStatus(EntryStatus.REGISTRADO);
        reversal.setReversalOfEntry(original);

        for (JournalEntryLine originalLine : original.getLines()) {
            MovementType invertedType = originalLine.getMovementType().opposite();
            AccountingAccount account = resolveDetailAccount(originalLine.getAccount().getAccountCode());
            account.applyMovement(invertedType, originalLine.getAmount());

            JournalEntryLine reversalLine = new JournalEntryLine();
            reversalLine.setAccount(account);
            reversalLine.setMovementType(invertedType);
            reversalLine.setAmount(originalLine.getAmount());
            reversalLine.setReference(originalLine.getReference());
            reversal.addLine(reversalLine);
        }

        original.setStatus(EntryStatus.ANULADO);
        journalEntryRepository.save(original);
        JournalEntry saved = journalEntryRepository.save(reversal);

        return JournalEntryMapper.toDetailDto(saved, null);
    }

    @Transactional(readOnly = true)
    public JournalEntryDetailDto getEntryDetail(String entryUuid) {
        return toDetailDtoWithReversal(findEntryOrThrow(entryUuid));
    }

    @Transactional(readOnly = true)
    public Page<JournalEntryDetailDto> listEntries(LocalDate from, LocalDate to, String status,
                                                    String accountCode, Pageable pageable) {
        Specification<JournalEntry> spec = buildEntrySpecification(from, to, status, accountCode);
        return journalEntryRepository.findAll(spec, pageable).map(this::toDetailDtoWithReversal);
    }

    private JournalEntry findEntryOrThrow(String entryUuid) {
        return journalEntryRepository.findByEntryUuid(entryUuid)
                .orElseThrow(() -> new EntryNotFoundException("El asiento " + entryUuid + " no existe."));
    }

    private JournalEntryDetailDto toDetailDtoWithReversal(JournalEntry entry) {
        JournalEntry reversedBy = journalEntryRepository.findByReversalOfEntry_Id(entry.getId()).orElse(null);
        return JournalEntryMapper.toDetailDto(entry, reversedBy);
    }

    private Specification<JournalEntry> buildEntrySpecification(LocalDate from, LocalDate to, String status,
                                                                 String accountCode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("entryDate"), to.plusDays(1).atStartOfDay()));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), EntryStatus.valueOf(status)));
            }
            if (accountCode != null && !accountCode.isBlank()) {
                query.distinct(true);
                predicates.add(cb.equal(root.join("lines").get("account").get("accountCode"), accountCode));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

}
