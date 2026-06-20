package ec.edu.espe.banquito.accountservice.mapper;

import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.model.JournalEntry;

public class JournalEntryMapper {

    private JournalEntryMapper() {
    }

    public static JournalEntryResponse toResponse(JournalEntry entry) {
        return new JournalEntryResponse(
                entry.getId(),
                entry.getEntryUuid(),
                entry.getStatus().name(),
                "SUMA_CERO_OK",
                entry.getEntryDate());
    }
}
