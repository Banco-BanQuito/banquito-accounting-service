package ec.edu.espe.banquito.accountservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Parámetro operativo del servicio contable (tabla {@code accounting_parameter}).
 * Clave usada: {@code FECHA_CONTABLE_ACTIVA}, que avanza en cada cierre EOD.
 */
@Entity
@Table(name = "accounting_parameter")
public class AccountingParameter {

    @Id
    @Column(name = "param_key", length = 50)
    private String paramKey;

    @Column(name = "param_value", nullable = false, length = 100)
    private String paramValue;

    protected AccountingParameter() {
    }

    public AccountingParameter(String paramKey, String paramValue) {
        this.paramKey = paramKey;
        this.paramValue = paramValue;
    }

    public String getParamKey() {
        return paramKey;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }
}
