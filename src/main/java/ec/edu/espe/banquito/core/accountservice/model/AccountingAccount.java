package ec.edu.espe.banquito.core.accountservice.model;

import ec.edu.espe.banquito.core.accountservice.enums.AccountNature;
import ec.edu.espe.banquito.core.accountservice.enums.AccountType;
import ec.edu.espe.banquito.core.accountservice.enums.MovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_account")
@Getter
@Setter
public class AccountingAccount {

    @Id
    @Column(name = "account_code", length = 20)
    private String accountCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "account_class", nullable = false, length = 15)
    private String accountClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 15)
    private AccountType accountType;

    @Column(name = "parent_account_code", length = 20)
    private String parentAccountCode;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    /**
     * Devuelve la NATURALEZA de esta cuenta (DEUDORA o ACREEDORA).
     *
     * <p>No es una columna nueva en la base de datos: la naturaleza se DEDUCE de la clase
     * contable (ACTIVO, PASIVO, etc.). Así evitamos guardar un dato que ya se puede calcular
     * a partir de la clase, y no puede quedar "desincronizado".</p>
     */
    public AccountNature getNature() {
        return AccountNature.fromClass(this.accountClass);
    }

    /**
     * APLICA UN MOVIMIENTO (débito o crédito) AL SALDO DE LA CUENTA.
     *
     * <p>Idea clave: el saldo (currentBalance) siempre se guarda como un número POSITIVO que
     * representa cuánto hay en el LADO NORMAL de la cuenta (su naturaleza). No usamos signos
     * para distinguir deudoras de acreedoras.</p>
     *
     * <p>La regla es "una cuenta sube por su propio lado":</p>
     * <ul>
     *   <li>Cuenta DEUDORA (ACTIVO/GASTO): un DEBITO SUMA, un CREDITO RESTA.</li>
     *   <li>Cuenta ACREEDORA (PASIVO/PATRIMONIO/INGRESO): un CREDITO SUMA, un DEBITO RESTA.</li>
     * </ul>
     *
     * <p>Dicho de otra forma: si el movimiento cae en el lado normal de la cuenta, sumamos;
     * si cae en el lado contrario, restamos. Por eso el saldo normalmente queda en cero o
     * positivo.</p>
     */
    public void applyMovement(MovementType movementType, BigDecimal amount) {
        // Averiguamos cuál es el lado "que suma" según la naturaleza de la cuenta.
        MovementType ladoQueSuma =
                getNature() == AccountNature.DEUDORA ? MovementType.DEBITO : MovementType.CREDITO;

        if (movementType == ladoQueSuma) {
            // El movimiento cae en el lado normal de la cuenta: el saldo crece.
            this.currentBalance = this.currentBalance.add(amount);
        } else {
            // El movimiento cae en el lado contrario: el saldo baja.
            this.currentBalance = this.currentBalance.subtract(amount);
        }
    }

    public boolean isDetail() {
        return this.accountType == AccountType.DETALLE;
    }
}
