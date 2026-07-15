package ec.edu.espe.banquito.core.accountservice.repository;

import ec.edu.espe.banquito.core.accountservice.model.JournalEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long>,
        JpaSpecificationExecutor<JournalEntry> {

    Optional<JournalEntry> findByEntryUuid(String entryUuid);

    boolean existsByEntryUuid(String entryUuid);

    Optional<JournalEntry> findByReversalOfEntry_Id(Long reversalOfEntryId);
}
