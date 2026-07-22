package ec.edu.espe.banquito.core.accountservice.dto;

import java.time.LocalDate;
import java.util.List;

public record JournalEntryRequest(
        String entryUuid,
        String description,
        LocalDate entryDate,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String beneficiaryName,
        List<JournalEntryLineRequest> lines) {

    public JournalEntryRequest(String entryUuid, String description, LocalDate entryDate,
                               List<JournalEntryLineRequest> lines) {
        this(entryUuid, description, entryDate, null, null, null, lines);
    }
}
