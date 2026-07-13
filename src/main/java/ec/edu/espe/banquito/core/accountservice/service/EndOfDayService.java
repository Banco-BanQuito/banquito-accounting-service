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

/**
 * EL "CIERRE DE CAJA" DEL DÍA (End Of Day).
 *
 * <p>Es como cuando una tienda cierra al final del día: cuenta todo, revisa que las
 * cuentas cuadren y prepara todo para mañana. Este proceso hace exactamente eso con el
 * banco entero:</p>
 * <ol>
 *   <li>Arma el reporte resumido del día.</li>
 *   <li>Verifica que todo cuadre (que no falte ni sobre dinero en los libros).</li>
 *   <li>Si cuadra: guarda el reporte en un archivo y pasa la fecha al día siguiente.</li>
 *   <li>Si NO cuadra: detiene el cierre y avisa del problema, para no arrastrar errores.</li>
 * </ol>
 */
@Service
public class EndOfDayService {

    /** Formato de fecha para nombrar el archivo del reporte, ej. "20260705" (año-mes-día pegados). */
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** Marca invisible al inicio del archivo para que Excel muestre bien las tildes y la ñ. */
    private static final String BOM = new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);

    /** El libro contable, a quien le pedimos el reporte resumido del día. */
    private final AccountingService accountingService;
    /** Nos deja leer qué día está abierto y avanzarlo al siguiente cuando cerramos. */
    private final ParameterService parameterService;
    /** La carpeta del computador donde se van guardando los reportes de cada cierre. */
    private final Path reportsDir;

    public EndOfDayService(AccountingService accountingService,
                           ParameterService parameterService,
                           @Value("${accounting.reports.dir:./reports}") String reportsDir) {
        this.accountingService = accountingService;
        this.parameterService = parameterService;
        this.reportsDir = Path.of(reportsDir);
    }

    /**
     * EJECUTA EL CIERRE DEL DÍA (el método principal).
     *
     * <p>Pide el reporte resumido del día. Si NO cuadra, se detiene ahí mismo, avisa del
     * problema y no cierra nada (así no se cierra un día con errores). Si SÍ cuadra, guarda
     * el reporte en un archivo, adelanta el calendario contable al día siguiente y devuelve
     * un resumen del cierre: qué día se cerró, cuál es el nuevo día, los totales y dónde
     * quedó guardado el reporte.</p>
     */
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

    /**
     * Guarda el reporte del día en un archivo tipo Excel (CSV): una fila por cada cuenta y
     * al final una fila con los totales. El archivo se guarda en la carpeta de reportes y
     * se prepara para que Excel lo abra sin problemas de tildes. Devuelve la ubicación
     * exacta donde quedó guardado.
     */
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
