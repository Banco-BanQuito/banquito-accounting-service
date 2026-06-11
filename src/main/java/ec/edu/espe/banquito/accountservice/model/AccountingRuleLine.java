package ec.edu.espe.banquito.accountservice.model;

import ec.edu.espe.banquito.accountservice.enums.AmountComponent;
import ec.edu.espe.banquito.accountservice.enums.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "accounting_rule_line")
@Getter
@Setter
public class AccountingRuleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AccountingRule rule;

    @Column(name = "line_order", nullable = false)
    private Integer lineOrder;

    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_component", nullable = false, length = 25)
    private AmountComponent amountComponent;

    @Column(name = "skip_if_zero", nullable = false)
    private boolean skipIfZero = true;
}
