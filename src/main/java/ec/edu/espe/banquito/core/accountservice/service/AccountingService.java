package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.enums.AccountNature;
import ec.edu.espe.banquito.core.accountservice.enums.AccountType;
import ec.edu.espe.banquito.core.accountservice.enums.EntryStatus;
import ec.edu.espe.banquito.core.accountservice.enums.MovementType;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.core.accountservice.mapper.JournalEntryMapper;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceAccountDto;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.exception.InvalidAccountException;
import ec.edu.espe.banquito.core.accountservice.exception.UnbalancedEntryException;
import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.core.accountservice.model.JournalEntry;
import ec.edu.espe.banquito.core.accountservice.model.JournalEntryLine;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.core.accountservice.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EL LIBRO CONTABLE DEL BANCO.
 *
 * <p>Esta clase es el "cuaderno de contabilidad" oficial de BanQuito. Cada vez que pasa
 * algo con dinero (un depósito, un retiro, un cobro de comisión), aquí se anota en dos
 * columnas: lo que ENTRA y lo que SALE. En contabilidad a eso se le llama "partida
 * doble", y la regla de oro es que las dos columnas siempre deben sumar lo mismo.</p>
 *
 * <p>Se encarga de tres cosas:</p>
 * <ul>
 *   <li>Anotar los movimientos de dinero (llamados "asientos").</li>
 *   <li>Actualizar el saldo de cada cuenta afectada.</li>
 *   <li>Armar el "Balance de Comprobación": el reporte que demuestra que todo cuadra.</li>
 * </ul>
 */
@Service
public class AccountingService {

    /** Nos da acceso a la lista de todas las cuentas del banco y cuánto dinero tiene cada una. */
    private final AccountingAccountRepository accountRepository;
    /** Guarda cada movimiento de dinero anotado, con su detalle de entradas y salidas. */
    private final JournalEntryRepository journalEntryRepository;
    /** Nos dice la configuración del sistema, por ejemplo qué día contable está abierto hoy. */
    private final ParameterService parameterService;

    public AccountingService(AccountingAccountRepository accountRepository,
                             JournalEntryRepository journalEntryRepository,
                             ParameterService parameterService) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.parameterService = parameterService;
    }

    /**
     * ANOTAR UN MOVIMIENTO DE DINERO EN EL LIBRO.
     *
     * <p>Este es el método principal. Recibe un movimiento (por ejemplo "entraron $100 a
     * la cuenta A y salieron $100 de la cuenta B") y lo anota. Antes de anotar, revisa
     * que todo esté bien y que las entradas sumen igual que las salidas.</p>
     *
     * <p>Detalle importante: si por error se pide anotar DOS VECES el mismo movimiento,
     * no lo duplica. Cada movimiento trae un código único; si ya existe uno con ese
     * código, simplemente devuelve el que ya estaba. Esto evita cobrar o abonar dos
     * veces si el sistema reintenta la petición.</p>
     */
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

    /**
     * ARMAR EL REPORTE QUE DEMUESTRA QUE TODO CUADRA (detallado).
     *
     * <p>Revisa cuenta por cuenta cuánto dinero tiene cada una y las organiza en dos
     * columnas: las que tienen saldo a favor y las que tienen saldo en contra. Al final
     * suma cada columna. Si las dos columnas dan el mismo total, el banco está "cuadrado"
     * (no falta ni sobra dinero en los libros).</p>
     *
     * <p>Esta versión muestra TODAS las cuentas una por una, con lujo de detalle.</p>
     */
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

    /**
     * ARMAR EL MISMO REPORTE, PERO RESUMIDO POR GRUPOS.
     *
     * <p>Es como el reporte anterior, pero en lugar de mostrar cuenta por cuenta, agrupa
     * las cuentas por categorías grandes (activos, pasivos, ingresos, etc.) y muestra
     * solo los totales de cada grupo. Es la vista "de titulares" que se usa en el cierre
     * de día para revisar rápido, de un vistazo, si el banco entero cuadra.</p>
     */
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
                    // La columna se decide por la NATURALEZA de la cuenta, no por el signo del saldo.
                    // El saldo ya viene positivo en su lado normal (ver AccountingAccount.applyMovement):
                    //   cuenta DEUDORA (activo/gasto)   -> su saldo va en la columna DEUDORA.
                    //   cuenta ACREEDORA (pasivo/etc.)  -> su saldo va en la columna ACREEDORA.
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

    /** Toma una cuenta y la convierte en una fila del reporte, colocando su saldo en la columna correcta. */
    private TrialBalanceAccountDto toTrialBalanceRow(AccountingAccount account) {
        return toTrialBalanceRow(account.getAccountCode(), account.getName(),
                account.getAccountClass(), account.getCurrentBalance());
    }

    /**
     * Coloca un saldo en la columna correcta del Balance de Comprobación SEGÚN LA NATURALEZA
     * de la cuenta (no según el signo del número).
     *
     * <p>Con el nuevo modelo, el saldo ya viene positivo en su lado normal. Entonces:</p>
     * <ul>
     *   <li>Cuenta DEUDORA (activo/gasto): su saldo va en la columna "Saldo Deudor".</li>
     *   <li>Cuenta ACREEDORA (pasivo/patrimonio/ingreso): su saldo va en la columna "Saldo Acreedor".</li>
     * </ul>
     *
     * <p>Antes esto se decidía mirando si el número era positivo o negativo, lo cual clasificaba
     * mal cualquier cuenta que tuviera un saldo anómalo (por ejemplo un activo en sobregiro).
     * Ahora la clase de la cuenta manda, que es como funciona la contabilidad de verdad.</p>
     */
    private TrialBalanceAccountDto toTrialBalanceRow(String code, String name,
                                                     String accountClass, BigDecimal balance) {
        boolean esDeudora = AccountNature.fromClass(accountClass) == AccountNature.DEUDORA;
        BigDecimal debit = esDeudora ? balance : BigDecimal.ZERO;
        BigDecimal credit = esDeudora ? BigDecimal.ZERO : balance;
        return new TrialBalanceAccountDto(code, name, debit, credit);
    }

    /**
     * Busca la cuenta por su código y confirma dos cosas antes de tocarla: que exista, y
     * que sea una cuenta "de detalle" (las únicas que pueden recibir movimientos de
     * dinero). Las cuentas que solo agrupan a otras no se pueden mover directamente.
     */
    private AccountingAccount resolveDetailAccount(String code) {
        AccountingAccount account = accountRepository.findByIdForUpdate(code)
                .orElseThrow(() -> new InvalidAccountException("La cuenta " + code + " no existe en el Plan de Cuentas."));
        if (!account.isDetail()) {
            throw new InvalidAccountException("La cuenta " + code + " no es de tipo DETALLE; no puede recibir asientos.");
        }
        return account;
    }

    /**
     * Revisa que el movimiento venga bien formado antes de anotarlo, como una lista de
     * chequeo: que traiga su código único, que tenga al menos una línea, y que cada línea
     * diga a qué cuenta afecta, si es entrada o salida, y un monto mayor que cero.
     */
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

    /**
     * La regla de oro de la contabilidad: lo que ENTRA debe ser igual a lo que SALE.
     * Suma todas las entradas, suma todas las salidas y compara. Si no dan exactamente
     * lo mismo, rechaza el movimiento y no lo anota.
     */
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
