package ec.edu.espe.banquito.accountservice.service;

import ec.edu.espe.banquito.accountservice.domain.AccountingParameter;
import ec.edu.espe.banquito.accountservice.repository.AccountingParameterRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** Acceso a los parámetros operativos del servicio contable. */
@Service
public class ParameterService {

    public static final String FECHA_CONTABLE_ACTIVA = "FECHA_CONTABLE_ACTIVA";

    private final AccountingParameterRepository parameterRepository;

    public ParameterService(AccountingParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    /** Fecha contable activa del banco. Si no está configurada, usa la fecha de hoy. */
    public LocalDate getActiveContableDate() {
        return parameterRepository.findById(FECHA_CONTABLE_ACTIVA)
                .map(p -> LocalDate.parse(p.getParamValue()))
                .orElse(LocalDate.now());
    }

    /** Avanza la fecha contable activa a la fecha indicada (usado por el cierre EOD). */
    public void setActiveContableDate(LocalDate date) {
        AccountingParameter param = parameterRepository.findById(FECHA_CONTABLE_ACTIVA)
                .orElseGet(() -> new AccountingParameter(FECHA_CONTABLE_ACTIVA, date.toString()));
        param.setParamValue(date.toString());
        parameterRepository.save(param);
    }
}
