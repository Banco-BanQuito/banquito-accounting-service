package ec.edu.espe.banquito.accountservice.repository;

import ec.edu.espe.banquito.accountservice.domain.AccountingAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, Long> {

    Optional<AccountingAccount> findByCode(String code);

    List<AccountingAccount> findAllByOrderByCodeAsc();
}
