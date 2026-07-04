package ec.edu.espe.banquito.core.accountservice.repository;

import ec.edu.espe.banquito.core.accountservice.model.JournalEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByEntryUuid(String entryUuid);

    boolean existsByEntryUuid(String entryUuid);
}
