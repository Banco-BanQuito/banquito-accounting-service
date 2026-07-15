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

    public AccountNature getNature() {
        return AccountNature.fromClass(this.accountClass);
    }

    public void applyMovement(MovementType movementType, BigDecimal amount) {
        MovementType ladoQueSuma =
                getNature() == AccountNature.DEUDORA ? MovementType.DEBITO : MovementType.CREDITO;

        if (movementType == ladoQueSuma) {
            this.currentBalance = this.currentBalance.add(amount);
        } else {
            this.currentBalance = this.currentBalance.subtract(amount);
        }
    }

    public boolean isDetail() {
        return this.accountType == AccountType.DETALLE;
    }
}
