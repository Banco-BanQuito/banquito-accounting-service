package ec.edu.espe.banquito.core.accountservice.repository;

import ec.edu.espe.banquito.core.accountservice.model.JournalEntry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long>,
        JpaSpecificationExecutor<JournalEntry> {

    Optional<JournalEntry> findByEntryUuid(String entryUuid);

    boolean existsByEntryUuid(String entryUuid);

    Optional<JournalEntry> findByReversalOfEntry_Id(Long reversalOfEntryId);

    /**
     * Saldo acumulado por cuenta hasta el final del dia indicado, calculado desde los
     * asientos y no desde AccountingAccount.currentBalance.
     *
     * El balance de comprobacion es un corte historico: debe reflejar la posicion a esa
     * fecha, no la posicion actual. Usar currentBalance producia un reporte fechado en el
     * pasado pero con cifras de hoy.
     *
     * Solo cuenta asientos REGISTRADO: los ANULADO ya tienen su asiento de reverso
     * registrado aparte, sumarlos ademas duplicaria el efecto de la anulacion.
     *
     * El signo devuelto es (DEBITO - CREDITO), convencion de cuenta deudora. Las cuentas
     * acreedoras (pasivo, patrimonio, ingresos) deben invertirlo al presentarlo.
     */
    @Query("""
            SELECT l.account.accountCode,
                   SUM(CASE WHEN l.movementType = ec.edu.espe.banquito.core.accountservice.enums.MovementType.DEBITO
                            THEN l.amount ELSE -l.amount END)
              FROM JournalEntryLine l
             WHERE l.journalEntry.entryDate < :endExclusive
               AND l.journalEntry.status = ec.edu.espe.banquito.core.accountservice.enums.EntryStatus.REGISTRADO
             GROUP BY l.account.accountCode
            """)
    List<Object[]> sumBalancesUntil(@Param("endExclusive") LocalDateTime endExclusive);
}
