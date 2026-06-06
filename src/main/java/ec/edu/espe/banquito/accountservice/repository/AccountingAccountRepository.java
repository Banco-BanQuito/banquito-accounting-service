package ec.edu.espe.banquito.accountservice.repository;

import ec.edu.espe.banquito.accountservice.model.AccountingAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, String> {

    List<AccountingAccount> findAllByOrderByAccountCodeAsc();
}
