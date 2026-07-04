package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.model.AccountingParameter;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingParameterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class ParameterService {

    public static final String FECHA_CONTABLE_ACTIVA = "FECHA_CONTABLE_ACTIVA";
    public static final String IVA_RATE = "IVA_RATE";

    private final AccountingParameterRepository parameterRepository;

    public ParameterService(AccountingParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    public LocalDate getActiveContableDate() {
        return parameterRepository.findById(FECHA_CONTABLE_ACTIVA)
                .map(p -> LocalDate.parse(p.getParamValue()))
                .orElse(LocalDate.now(ZoneOffset.UTC));
    }

    public void setActiveContableDate(LocalDate date) {
        setParameter(FECHA_CONTABLE_ACTIVA, date.toString());
    }

    public BigDecimal getIvaRate() {
        return parameterRepository.findById(IVA_RATE)
                .map(p -> new BigDecimal(p.getParamValue()))
                .orElse(new BigDecimal("0.15"));
    }

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
