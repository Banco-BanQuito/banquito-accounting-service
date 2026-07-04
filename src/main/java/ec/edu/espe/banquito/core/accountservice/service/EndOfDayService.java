package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.dto.EodRequest;
import ec.edu.espe.banquito.core.accountservice.dto.EodResponse;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceAccountDto;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceResponse;
import ec.edu.espe.banquito.core.accountservice.exception.EodNotBalancedException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EndOfDayService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String BOM = new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);

    private final AccountingService accountingService;
    private final ParameterService parameterService;
    private final Path reportsDir;

    public EndOfDayService(AccountingService accountingService,
                           ParameterService parameterService,
                           @Value("${accounting.reports.dir:./reports}") String reportsDir) {
        this.accountingService = accountingService;
        this.parameterService = parameterService;
        this.reportsDir = Path.of(reportsDir);
    }

    @Transactional
    public EodResponse runEndOfDay(EodRequest request) {
        LocalDate contableDate = request != null && request.contableDate() != null
                ? request.contableDate()
                : parameterService.getActiveContableDate();

        TrialBalanceResponse balance = accountingService.structuralTrialBalance(contableDate);

        if (!balance.balanced()) {
            throw new EodNotBalancedException(
                    "No se puede cerrar el día " + contableDate + ": débitos=" + balance.totalDebits()
                            + " créditos=" + balance.totalCredits() + " no cuadran.");
        }

        String reportPath = writeTrialBalanceCsv(contableDate, balance);
        LocalDate nextContableDate = contableDate.plusDays(1);
        parameterService.setActiveContableDate(nextContableDate);

        return new EodResponse(
                "COMPLETADO",
                contableDate,
                nextContableDate,
                balance.totalDebits(),
                balance.totalCredits(),
                "CUADRADO",
                reportPath);
    }

    private String writeTrialBalanceCsv(LocalDate date, TrialBalanceResponse balance) {
        List<String> rows = new ArrayList<>();
        rows.add("Código de Cuenta,Nombre de Cuenta,Saldo Deudor,Saldo Acreedor");
        for (TrialBalanceAccountDto account : balance.accounts()) {
            rows.add(String.join(",",
                    account.code(),
                    "\"" + account.name() + "\"",
                    account.debitBalance().toPlainString(),
                    account.creditBalance().toPlainString()));
        }
        rows.add(String.join(",",
                "TOTAL", "\"\"",
                balance.totalDebits().toPlainString(),
                balance.totalCredits().toPlainString()));

        try {
            Files.createDirectories(reportsDir);
            Path file = reportsDir.resolve("balance_" + date.format(FILE_DATE) + ".csv");
            String content = BOM + String.join("\r\n", rows) + "\r\n";
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el CSV del Balance de Comprobación", e);
        }
    }
}
