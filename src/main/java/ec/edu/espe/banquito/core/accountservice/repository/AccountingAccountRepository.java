package ec.edu.espe.banquito.core.accountservice.repository;

import ec.edu.espe.banquito.core.accountservice.enums.AccountType;
import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, String> {

    List<AccountingAccount> findAllByOrderByAccountCodeAsc();

    List<AccountingAccount> findByAccountTypeOrderByAccountCodeAsc(AccountType accountType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountingAccount a where a.accountCode = :accountCode")
    Optional<AccountingAccount> findByIdForUpdate(@Param("accountCode") String accountCode);
}
