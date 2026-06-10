package ec.edu.espe.banquito.accountservice.model;

import ec.edu.espe.banquito.accountservice.enums.EntryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entry")
@Getter
@Setter
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_uuid", nullable = false, unique = true, length = 36)
    private String entryUuid;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "entry_date", nullable = false)
    private LocalDateTime entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EntryStatus status;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();

    public void addLine(JournalEntryLine line) {
        line.setJournalEntry(this);
        this.lines.add(line);
    }
}
