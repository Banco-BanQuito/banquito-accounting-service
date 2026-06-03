package ec.edu.espe.banquito.accountservice.repository;

import ec.edu.espe.banquito.accountservice.domain.JournalEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByEntryUuid(String entryUuid);

    boolean existsByEntryUuid(String entryUuid);
}
