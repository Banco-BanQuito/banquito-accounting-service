package ec.edu.espe.banquito.core.accountservice.mapper;

import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryDetailDto;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryLineDto;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.core.accountservice.enums.MovementType;
import ec.edu.espe.banquito.core.accountservice.model.JournalEntry;
import ec.edu.espe.banquito.core.accountservice.model.JournalEntryLine;
import java.math.BigDecimal;
import java.util.List;

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

    public static JournalEntryDetailDto toDetailDto(JournalEntry entry, JournalEntry reversedByEntry) {
        List<JournalEntryLineDto> lines = entry.getLines().stream()
                .map(line -> new JournalEntryLineDto(
                        line.getAccount().getAccountCode(),
                        line.getAccount().getName(),
                        line.getMovementType().name(),
                        line.getAmount(),
                        line.getReference()))
                .toList();

        BigDecimal totalDebit = sumByType(entry.getLines(), MovementType.DEBITO);
        BigDecimal totalCredit = sumByType(entry.getLines(), MovementType.CREDITO);

        return new JournalEntryDetailDto(
                entry.getId(),
                entry.getEntryUuid(),
                entry.getDescription(),
                entry.getEntryDate(),
                entry.getStatus().name(),
                lines,
                totalDebit,
                totalCredit,
                totalDebit.compareTo(totalCredit) == 0,
                entry.getReversalOfEntry() != null ? entry.getReversalOfEntry().getEntryUuid() : null,
                reversedByEntry != null ? reversedByEntry.getEntryUuid() : null);
    }

    private static BigDecimal sumByType(List<JournalEntryLine> lines, MovementType type) {
        return lines.stream()
                .filter(line -> line.getMovementType() == type)
                .map(JournalEntryLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
