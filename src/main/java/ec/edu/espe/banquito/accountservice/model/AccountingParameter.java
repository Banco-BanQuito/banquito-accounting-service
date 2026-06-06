package ec.edu.espe.banquito.accountservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "accounting_parameter")
@Getter
@Setter
public class AccountingParameter {

    @Id
    @Column(name = "param_key", length = 50)
    private String paramKey;

    @Column(name = "param_value", nullable = false, length = 100)
    private String paramValue;
}
