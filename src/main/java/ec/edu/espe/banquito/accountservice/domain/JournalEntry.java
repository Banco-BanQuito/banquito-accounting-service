package ec.edu.espe.banquito.accountservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de un asiento contable (tabla {@code journal_entry}).
 * <p>
 * Un asiento es inmutable una vez registrado; las correcciones se hacen con un
 * asiento compensatorio. El {@code entryUuid} garantiza idempotencia ante
 * reintentos de los servicios que lo invocan (account-core y clearinghouse).
 */
@Entity
@Table(name = "journal_entry")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_uuid", nullable = false, unique = true, length = 64)
    private String entryUuid;

    @Column(length = 250)
    private String description;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();

    protected JournalEntry() {
    }

    public JournalEntry(String entryUuid, String description, LocalDate entryDate) {
        this.entryUuid = entryUuid;
        this.description = description;
        this.entryDate = entryDate;
        this.status = EntryStatus.REGISTRADO;
        this.createdAt = Instant.now();
    }

    public void addLine(JournalEntryLine line) {
        line.setJournalEntry(this);
        this.lines.add(line);
    }

    public Long getId() {
        return id;
    }

    public String getEntryUuid() {
        return entryUuid;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public EntryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<JournalEntryLine> getLines() {
        return lines;
    }
}
