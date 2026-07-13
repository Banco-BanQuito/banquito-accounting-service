package ec.edu.espe.banquito.core.accountservice.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceAccountDto;
import ec.edu.espe.banquito.core.accountservice.dto.TrialBalanceResponse;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * LA "IMPRENTA" DE REPORTES.
 *
 * <p>Toma el reporte del balance y lo convierte en un archivo que la persona puede
 * descargar, en dos presentaciones:</p>
 * <ul>
 *   <li><b>CSV:</b> un archivo tipo Excel, para quien quiera manipular los números.</li>
 *   <li><b>PDF:</b> un documento con la imagen corporativa de BanQuito (logo, tabla
 *       ordenada, colores, y un sello que dice si cuadra o no), listo para presentar.</li>
 * </ul>
 *
 * <p>Diferencia con el cierre de día: allá el archivo se guarda en el computador; aquí,
 * en cambio, el reporte se entrega al instante para descargarlo desde el sistema.</p>
 */
@Service
public class ReportService {

    /** Marca invisible al inicio del archivo para que Excel muestre bien las tildes y la ñ. */
    private static final String BOM = new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);
    /** Formato de la fecha y hora que aparece al pie del PDF ("generado el..."). */
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Arma el reporte en formato Excel (CSV): una fila por cuenta y una fila final con los
     * totales. Devuelve el archivo listo para descargar.
     */
    public byte[] generateCsvBytes(TrialBalanceResponse balance) {
        List<String> rows = new ArrayList<>();
        rows.add("Código de Cuenta,Nombre de Cuenta,Saldo Deudor,Saldo Acreedor");
        for (TrialBalanceAccountDto account : balance.accounts()) {
            rows.add(String.join(",",
                    account.code(),
                    "\"" + account.name().replace("\"", "\"\"") + "\"",
                    account.debitBalance().toPlainString(),
                    account.creditBalance().toPlainString()));
        }
        rows.add(String.join(",",
                "TOTAL", "\"\"",
                balance.totalDebits().toPlainString(),
                balance.totalCredits().toPlainString()));

        String content = BOM + String.join("\r\n", rows) + "\r\n";
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Arma el reporte en formato PDF, bonito y presentable, con la imagen de BanQuito:
     * el nombre del banco, la fecha, la hora en que se generó, un sello que dice si
     * "CUADRADO" o "DESCUADRADO", y una tabla con las cuentas. La fila de totales se pinta
     * de verde si todo cuadra o de rojo si no. Devuelve el documento listo para descargar.
     */
    public byte[] generatePdfBytes(TrialBalanceResponse balance) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try (AutoCloseable closer = () -> { if (document.isOpen()) document.close(); }) {
            PdfWriter.getInstance(document, out);
            document.open();

            Font bankFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(30, 58, 138));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 41, 59));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(30, 41, 59));
            Font monoFont = FontFactory.getFont(FontFactory.COURIER, 9, new Color(30, 41, 59));
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 41, 59));
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(148, 163, 184));

            Paragraph bank = new Paragraph("BanQuito", bankFont);
            bank.setAlignment(Element.ALIGN_CENTER);
            document.add(bank);

            Paragraph title = new Paragraph("Balance de Comprobación", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(4);
            document.add(title);

            String balanceLabel = balance.balanced() ? "✓ CUADRADO" : "✗ DESCUADRADO";
            Color balanceColor = balance.balanced() ? new Color(21, 128, 61) : new Color(185, 28, 28);
            Font balanceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, balanceColor);

            Paragraph meta = new Paragraph("Fecha contable: " + balance.contableDate()
                    + "     Generado: " + LocalDateTime.now(ZoneId.of("America/Guayaquil")).format(DT_FMT)
                    + "     Estado: ", subtitleFont);
            meta.add(new Phrase(balanceLabel, balanceFont));
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingBefore(6);
            meta.setSpacingAfter(16);
            document.add(meta);

            PdfPTable table = new PdfPTable(new float[]{2.5f, 5f, 2.5f, 2.5f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(4);

            String[] headers = {"Código de Cuenta", "Nombre de Cuenta", "Saldo Deudor", "Saldo Acreedor"};
            Color headerBg = new Color(30, 58, 138);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(new Color(30, 58, 138));
                table.addCell(cell);
            }

            boolean alternate = false;
            Color altBg = new Color(248, 250, 252);
            Color borderColor = new Color(226, 232, 240);

            for (TrialBalanceAccountDto acc : balance.accounts()) {
                Color rowBg = alternate ? altBg : Color.WHITE;
                alternate = !alternate;

                addCell(table, acc.code(), monoFont, rowBg, borderColor, Element.ALIGN_LEFT);
                addCell(table, acc.name(), rowFont, rowBg, borderColor, Element.ALIGN_LEFT);
                addCell(table, acc.debitBalance().compareTo(BigDecimal.ZERO) > 0
                        ? "$" + fmt(acc.debitBalance()) : "—", rowFont, rowBg, borderColor, Element.ALIGN_RIGHT);
                addCell(table, acc.creditBalance().compareTo(BigDecimal.ZERO) > 0
                        ? "$" + fmt(acc.creditBalance()) : "—", rowFont, rowBg, borderColor, Element.ALIGN_RIGHT);
            }

            Color totalBg = balance.balanced() ? new Color(240, 253, 244) : new Color(255, 241, 242);
            Color totalBorder = balance.balanced() ? new Color(134, 239, 172) : new Color(254, 202, 202);

            addCell(table, "TOTAL", totalFont, totalBg, totalBorder, Element.ALIGN_LEFT);
            addCell(table, "", totalFont, totalBg, totalBorder, Element.ALIGN_LEFT);
            addCell(table, "$" + fmt(balance.totalDebits()), totalFont, totalBg, totalBorder, Element.ALIGN_RIGHT);
            addCell(table, "$" + fmt(balance.totalCredits()), totalFont, totalBg, totalBorder, Element.ALIGN_RIGHT);

            document.add(table);

            Paragraph footer = new Paragraph(
                    "Documento generado automáticamente por el sistema Core Bancario BanQuito · banquito.edu.ec",
                    footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            document.add(footer);

        } catch (Exception e) {
            throw new IllegalStateException("Error generando el PDF del balance de comprobación", e);
        }

        return out.toByteArray();
    }

    /** Ayudante que crea y pega una celda en la tabla del PDF con su color de fondo, borde y alineación. */
    private void addCell(PdfPTable table, String text, Font font, Color bg, Color border, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(border);
        table.addCell(cell);
    }

    /** Da formato a los montos con separador de miles y dos decimales, ej. 1.234,50, para que se lean fácil. */
    private String fmt(BigDecimal value) {
        return String.format("%,.2f", value);
    }
}
