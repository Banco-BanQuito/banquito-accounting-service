package ec.edu.espe.banquito.core.accountservice.repository;

import ec.edu.espe.banquito.core.accountservice.model.AccountingParameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingParameterRepository extends JpaRepository<AccountingParameter, String> {
}
