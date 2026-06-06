package ec.edu.espe.banquito.accountservice.service;

import ec.edu.espe.banquito.accountservice.model.AccountingParameter;
import ec.edu.espe.banquito.accountservice.repository.AccountingParameterRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class ParameterService {

    public static final String FECHA_CONTABLE_ACTIVA = "FECHA_CONTABLE_ACTIVA";

    private final AccountingParameterRepository parameterRepository;

    public ParameterService(AccountingParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    public LocalDate getActiveContableDate() {
        return parameterRepository.findById(FECHA_CONTABLE_ACTIVA)
                .map(p -> LocalDate.parse(p.getParamValue()))
                .orElse(LocalDate.now());
    }

    public void setActiveContableDate(LocalDate date) {
        AccountingParameter param = parameterRepository.findById(FECHA_CONTABLE_ACTIVA).orElse(null);
        if (param == null) {
            param = new AccountingParameter();
            param.setParamKey(FECHA_CONTABLE_ACTIVA);
        }
        param.setParamValue(date.toString());
        parameterRepository.save(param);
    }
}
