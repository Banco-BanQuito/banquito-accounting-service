package ec.edu.espe.banquito.accountservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
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

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @PrePersist
    protected void onCreate() {
        if (this.creationDate == null) {
            this.creationDate = LocalDateTime.now();
        }
    }

    public void applyMovement(MovementType movementType, BigDecimal amount) {
        if (movementType == MovementType.DEBITO) {
            this.currentBalance = this.currentBalance.add(amount);
        } else {
            this.currentBalance = this.currentBalance.subtract(amount);
        }
    }

    public boolean isDetalle() {
        return this.accountType == AccountType.DETALLE;
    }
}
