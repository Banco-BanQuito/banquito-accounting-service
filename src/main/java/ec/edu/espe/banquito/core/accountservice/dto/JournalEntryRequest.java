package ec.edu.espe.banquito.core.accountservice.dto;

import java.time.LocalDate;
import java.util.List;

public record JournalEntryRequest(
        String entryUuid,
        String description,
        LocalDate entryDate,
        List<JournalEntryLineRequest> lines) {
}
