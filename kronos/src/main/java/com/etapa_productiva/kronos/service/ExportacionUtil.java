package com.etapa_productiva.kronos.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 🧰 Generación genérica de tablas a Excel (.xlsx) y PDF apaisado, reutilizada por
 * los módulos que exportan (Gestión Fichas, Gestión Aprendices). Recibe los títulos
 * de columna y las filas ya convertidas a texto.
 */
public final class ExportacionUtil {

    private static final Color VERDE_SENA = new Color(5, 112, 21);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExportacionUtil() {
    }

    public static byte[] excel(String nombreHoja, String[] titulos, List<String[]> filas) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(nombreHoja);

            XSSFFont fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());

            CellStyle estiloTitulo = libro.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row filaTitulos = hoja.createRow(0);
            for (int i = 0; i < titulos.length; i++) {
                Cell celda = filaTitulos.createCell(i);
                celda.setCellValue(titulos[i]);
                celda.setCellStyle(estiloTitulo);
            }

            int numeroFila = 1;
            for (String[] fila : filas) {
                Row row = hoja.createRow(numeroFila++);
                for (int i = 0; i < fila.length; i++) {
                    row.createCell(i).setCellValue(fila[i] != null ? fila[i] : "");
                }
            }

            for (int i = 0; i < titulos.length; i++) {
                hoja.autoSizeColumn(i);
            }

            libro.write(salida);
            return salida.toByteArray();
        }
    }

    public static byte[] pdf(String titulo, String[] titulos, List<String[]> filas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 16, 16, 22, 22);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        Paragraph encabezado = new Paragraph(titulo,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, VERDE_SENA));
        encabezado.setAlignment(Element.ALIGN_CENTER);
        documento.add(encabezado);

        Paragraph fecha = new Paragraph("Generado el " + LocalDate.now().format(FORMATO_FECHA),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(12);
        documento.add(fecha);

        PdfPTable tabla = new PdfPTable(titulos.length);
        tabla.setWidthPercentage(100);

        float tamano = titulos.length > 12 ? 5.5f : 7f;
        com.lowagie.text.Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, tamano, Color.WHITE);
        for (String tituloColumna : titulos) {
            PdfPCell celda = new PdfPCell(new Paragraph(tituloColumna, fuenteTitulo));
            celda.setBackgroundColor(VERDE_SENA);
            celda.setPadding(3);
            tabla.addCell(celda);
        }

        com.lowagie.text.Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, tamano);
        for (String[] fila : filas) {
            for (String valor : fila) {
                PdfPCell celda = new PdfPCell(new Paragraph(valor != null ? valor : "", fuenteCelda));
                celda.setPadding(2.5f);
                tabla.addCell(celda);
            }
        }

        documento.add(tabla);
        documento.close();
        return salida.toByteArray();
    }
}
