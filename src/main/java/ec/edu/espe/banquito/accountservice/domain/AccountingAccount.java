package ec.edu.espe.banquito.accountservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Cuenta del Plan de Cuentas (tabla {@code accounting_account}).
 * <p>
 * El {@code balance} se almacena con convención <strong>débito-positivo</strong>:
 * un DÉBITO lo incrementa y un CRÉDITO lo decrementa. Así, las cuentas de
 * naturaleza deudora (activos) tienen saldo positivo y las de naturaleza
 * acreedora (pasivos/ingresos) tienen saldo negativo. El Balance de
 * Comprobación deriva el saldo deudor/acreedor a partir del signo.
 */
@Entity
@Table(name = "accounting_account")
public class AccountingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    protected AccountingAccount() {
    }

    public AccountingAccount(String code, String name, AccountType accountType, BigDecimal balance) {
        this.code = code;
        this.name = name;
        this.accountType = accountType;
        this.balance = balance;
    }

    /** Aplica un movimiento al saldo según la convención débito-positivo. */
    public void applyMovement(MovementType movementType, BigDecimal amount) {
        if (movementType == MovementType.DEBITO) {
            this.balance = this.balance.add(amount);
        } else {
            this.balance = this.balance.subtract(amount);
        }
    }

    public boolean isDetalle() {
        return this.accountType == AccountType.DETALLE;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
