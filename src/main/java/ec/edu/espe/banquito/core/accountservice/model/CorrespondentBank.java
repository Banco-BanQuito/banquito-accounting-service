package ec.edu.espe.banquito.core.accountservice.model;

import ec.edu.espe.banquito.core.accountservice.enums.CorrespondentBankStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "correspondent_bank")
@Getter
@Setter
public class CorrespondentBank {

    @Id
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "nostro_account_code", nullable = false, unique = true, length = 20)
    private String nostroAccountCode;

    @Column(name = "vostro_account_code", nullable = false, unique = true, length = 20)
    private String vostroAccountCode;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CorrespondentBankStatus status = CorrespondentBankStatus.ACTIVO;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;
}
