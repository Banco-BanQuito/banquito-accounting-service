package ec.edu.espe.banquito.core.accountservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record JournalEntryDetailDto(
        Long entryId,
        String entryUuid,
        String description,
        LocalDateTime entryDate,
        String status,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String beneficiaryName,
        String debitAccount,
        String creditAccount,
        List<JournalEntryLineDto> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        String reversalOfEntryUuid,
        String reversedByEntryUuid) {
}
