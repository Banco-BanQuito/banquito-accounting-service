package ec.edu.espe.banquito.accountservice.repository;

import ec.edu.espe.banquito.accountservice.model.AccountingRule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountingRuleRepository extends JpaRepository<AccountingRule, Long> {

    @Query("SELECT r FROM AccountingRule r WHERE r.operationType = :type " +
           "AND r.effectiveFrom <= :date " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) " +
           "ORDER BY r.effectiveFrom DESC")
    List<AccountingRule> findActiveByType(@Param("type") String operationType,
                                          @Param("date") LocalDate date);
}
