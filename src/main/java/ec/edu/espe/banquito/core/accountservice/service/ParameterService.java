package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.model.AccountingParameter;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingParameterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

/**
 * EL PANEL DE CONFIGURACIÓN DEL SISTEMA.
 *
 * <p>Piensa en el tablero de ajustes de una app. Esta clase guarda y entrega los valores
 * de configuración del módulo contable. Los dos más importantes son:</p>
 * <ul>
 *   <li><b>La fecha contable activa:</b> el día que está "abierto" para registrar
 *       movimientos. Avanza automáticamente cada vez que se hace el cierre del día.</li>
 *   <li><b>El porcentaje de IVA:</b> el impuesto que se aplica sobre las comisiones.</li>
 * </ul>
 *
 * <p>Si algún valor no está configurado todavía, entrega uno por defecto razonable para
 * que el sistema no se quede sin funcionar.</p>
 */
@Service
public class ParameterService {

    /** Nombre con el que se guarda la fecha contable que está abierta actualmente. */
    public static final String FECHA_CONTABLE_ACTIVA = "FECHA_CONTABLE_ACTIVA";
    /** Nombre con el que se guarda el porcentaje de IVA que se cobra sobre las comisiones. */
    public static final String IVA_RATE = "IVA_RATE";

    /** Acceso a la tabla donde se guardan todos estos valores de configuración. */
    private final AccountingParameterRepository parameterRepository;

    public ParameterService(AccountingParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    /** Dice qué día contable está abierto. Si nadie lo ha configurado, asume la fecha de hoy. */
    public LocalDate getActiveContableDate() {
        return parameterRepository.findById(FECHA_CONTABLE_ACTIVA)
                .map(p -> LocalDate.parse(p.getParamValue()))
                .orElse(LocalDate.now(ZoneOffset.UTC));
    }

    /** Cambia cuál es el día contable abierto. Lo usa el cierre para "pasar la página" al día siguiente. */
    public void setActiveContableDate(LocalDate date) {
        setParameter(FECHA_CONTABLE_ACTIVA, date.toString());
    }

    /** Dice cuánto IVA se cobra. Si nadie lo configuró, asume 0.15, es decir el 15%. */
    public BigDecimal getIvaRate() {
        return parameterRepository.findById(IVA_RATE)
                .map(p -> new BigDecimal(p.getParamValue()))
                .orElse(new BigDecimal("0.15"));
    }

    /**
     * Guarda cualquier valor de configuración. Si ese valor ya existía, lo actualiza; y si
     * es la primera vez, lo crea. (Es el método genérico que usan los dos de arriba.)
     */
    public void setParameter(String key, String value) {
        AccountingParameter param = parameterRepository.findById(key)
                .orElseGet(() -> {
                    AccountingParameter p = new AccountingParameter();
                    p.setParamKey(key);
                    return p;
                });
        param.setParamValue(value);
        parameterRepository.save(param);
    }
}
