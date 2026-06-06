package ec.edu.espe.banquito.accountservice.repository;

import ec.edu.espe.banquito.accountservice.model.AccountingParameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingParameterRepository extends JpaRepository<AccountingParameter, String> {
}
