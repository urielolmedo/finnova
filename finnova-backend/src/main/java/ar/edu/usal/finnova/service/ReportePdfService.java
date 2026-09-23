package ar.edu.usal.finnova.service;

import ar.edu.usal.finnova.dto.CategoriaResumen;
import ar.edu.usal.finnova.dto.ResumenFinancieroResponse;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportePdfService {

    private final GraficoService graficoService;

    public byte[] generarPdfReporte(String tituloReporte, LocalDate desde, LocalDate hasta,
                                    String usuarioNombre, ResumenFinancieroResponse resumen,
                                    List<CategoriaResumen> porCategoria) throws Exception {

        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font fontTitulo = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(26, 43, 74));
        Font fontSubtitulo = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);
        Font fontSeccion = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(26, 43, 74));
        Font fontNormal = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font fontNormalBold = new Font(Font.HELVETICA, 10, Font.BOLD);

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Encabezado
        Paragraph titulo = new Paragraph("FinNova - " + tituloReporte, fontTitulo);
        document.add(titulo);
        document.add(new Paragraph(
                "Período: " + desde.format(df) + " al " + hasta.format(df) +
                        "  |  Generado: " + LocalDate.now().format(df) + "  |  Usuario: " + usuarioNombre,
                fontSubtitulo));
        document.add(Chunk.NEWLINE);

        // Resumen numerico
        document.add(new Paragraph("Resumen financiero", fontSeccion));
        document.add(Chunk.NEWLINE);

        Font fontIngresos = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(46, 125, 50));
        Font fontEgresos = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(198, 40, 40));
        Font fontBalance = new Font(Font.HELVETICA, 10, Font.BOLD,
                resumen.isSuperavit() ? new Color(46, 125, 50) : new Color(198, 40, 40));

        PdfPTable tablaResumen = new PdfPTable(2);
        tablaResumen.setWidthPercentage(60);
        agregarFilaTabla(tablaResumen, "Total ingresos", formatearMonto(resumen.getTotalIngresos()), fontNormal, fontIngresos);
        agregarFilaTabla(tablaResumen, "Total egresos", formatearMonto(resumen.getTotalEgresos()), fontNormal, fontEgresos);
        agregarFilaTabla(tablaResumen, "Balance (" + (resumen.isSuperavit() ? "superávit" : "déficit") + ")",
                formatearMonto(resumen.getBalance()), fontNormal, fontBalance);
        document.add(tablaResumen);
        document.add(Chunk.NEWLINE);

        // Grafico de barras ingresos vs egresos
        byte[] graficoBarras = graficoService.generarGraficoBarras(resumen.getTotalIngresos(), resumen.getTotalEgresos());
        Image imgBarras = Image.getInstance(graficoBarras);
        imgBarras.scaleToFit(400, 240);
        document.add(imgBarras);
        document.add(Chunk.NEWLINE);

        // Desglose por categoria, si hay datos
        if (porCategoria != null && !porCategoria.isEmpty()) {
            document.add(new Paragraph("Desglose por categoría", fontSeccion));
            document.add(Chunk.NEWLINE);
            PdfPTable tablaCategorias = new PdfPTable(3);
            tablaCategorias.setWidthPercentage(90);
            tablaCategorias.addCell(celdaEncabezado("Categoría", fontNormalBold));
            tablaCategorias.addCell(celdaEncabezado("Ingresos", fontNormalBold));
            tablaCategorias.addCell(celdaEncabezado("Egresos", fontNormalBold));
            for (CategoriaResumen c : porCategoria) {
                tablaCategorias.addCell(new com.lowagie.text.pdf.PdfPCell(new Phrase(c.getCategoriaNombre(), fontNormal)));
                tablaCategorias.addCell(new com.lowagie.text.pdf.PdfPCell(new Phrase(formatearMonto(c.getTotalIngresos()), fontNormal)));
                tablaCategorias.addCell(new com.lowagie.text.pdf.PdfPCell(new Phrase(formatearMonto(c.getTotalEgresos()), fontNormal)));
            }
            document.add(tablaCategorias);
            document.add(Chunk.NEWLINE);

            List<String> nombres = porCategoria.stream().map(CategoriaResumen::getCategoriaNombre).toList();
            List<BigDecimal> totalesParaTorta = porCategoria.stream()
                    .map(c -> c.getTotalIngresos().add(c.getTotalEgresos())).toList();
            byte[] graficoTorta = graficoService.generarGraficoTorta(nombres, totalesParaTorta);
            Image imgTorta = Image.getInstance(graficoTorta);
            imgTorta.scaleToFit(400, 280);
            document.add(imgTorta);
        }

        document.close();
        return baos.toByteArray();
    }

    private void agregarFilaTabla(PdfPTable tabla, String etiqueta, String valor, Font fontEtiqueta, Font fontValor) {
        tabla.addCell(new com.lowagie.text.pdf.PdfPCell(new Phrase(etiqueta, fontEtiqueta)));
        tabla.addCell(new com.lowagie.text.pdf.PdfPCell(new Phrase(valor, fontValor)));
    }

    private com.lowagie.text.pdf.PdfPCell celdaEncabezado(String texto, Font font) {
        com.lowagie.text.pdf.PdfPCell celda = new com.lowagie.text.pdf.PdfPCell(new Phrase(texto, font));
        celda.setBackgroundColor(new Color(230, 230, 230));
        return celda;
    }

    private String formatearMonto(BigDecimal monto) {
        return "$ " + monto.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }
}
