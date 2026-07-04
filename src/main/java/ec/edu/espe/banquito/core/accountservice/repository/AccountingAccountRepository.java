package ec.edu.espe.banquito.core.accountservice.repository;

import ec.edu.espe.banquito.core.accountservice.enums.AccountType;
import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, String> {

    List<AccountingAccount> findAllByOrderByAccountCodeAsc();

    List<AccountingAccount> findByAccountTypeOrderByAccountCodeAsc(AccountType accountType);
}
