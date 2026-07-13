package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.ReporteAprendizDto;
import com.etapa_productiva.kronos.entity.DocumentoSolicitud;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.DocumentoSolicitudRepository;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 📋 "Reporte Aprendiz": evidencia de los documentos requisito ya diligenciados y APROBADOS,
 * buscable por nombre, apellido, documento, tipo de documento, ficha y modalidad de contrato.
 * Usado tanto por el rol Registro como por el Administrador (mismo criterio, misma vista).
 */
@Service
public class ReporteAprendizService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Una fila por documento aprobado (un aprendiz con 3 documentos genera 3 filas)
    private static final String[] TITULOS = {
            "Tipo Documento", "Documento", "Apellidos", "Nombres", "Ficha", "Programa",
            "Modalidad Contrato", "Estado Solicitud", "Documento Aprobado", "Asunto", "Fecha Subida"
    };

    @Autowired
    private DocumentoSolicitudRepository documentoSolicitudRepository;

    @Transactional(readOnly = true)
    public List<ReporteAprendizDto> buscar(String nombre, String apellido, String documento,
                                           TipoDocumento tipoDocumento, String ficha, Long idSeccionFormato) {

        String nombreFiltro = normalizar(nombre);
        String apellidoFiltro = normalizar(apellido);
        String documentoFiltro = normalizar(documento);
        String fichaFiltro = normalizar(ficha);

        // Agrupa los documentos APROBADOS por solicitud: cada solicitud es un aprendiz con su
        // ficha y modalidad de contrato puntuales.
        Map<Long, List<DocumentoSolicitud>> porSolicitud = new LinkedHashMap<>();
        for (DocumentoSolicitud doc : documentoSolicitudRepository.findByEstadoValidacion(EstadoValidacion.APROBADO)) {
            SolicitudEtapaPractica solicitud = doc.getSolicitud();
            Usuario aprendiz = solicitud.getAprendizFicha().getUsuario();
            Ficha fichaAprendiz = solicitud.getAprendizFicha().getFicha();

            if (nombreFiltro != null && !normalizar(aprendiz.getNombre()).contains(nombreFiltro)) continue;
            if (apellidoFiltro != null && !normalizar(aprendiz.getApellido()).contains(apellidoFiltro)) continue;
            if (documentoFiltro != null && !normalizar(aprendiz.getDocumento()).contains(documentoFiltro)) continue;
            if (tipoDocumento != null && aprendiz.getTipoDocumento() != tipoDocumento) continue;
            if (fichaFiltro != null && !normalizar(fichaAprendiz.getNumeroFicha()).contains(fichaFiltro)) continue;
            if (idSeccionFormato != null && (solicitud.getSeccionFormato() == null
                    || !idSeccionFormato.equals(solicitud.getSeccionFormato().getIdSeccionFormato()))) continue;

            porSolicitud.computeIfAbsent(solicitud.getIdSolicitud(), k -> new ArrayList<>()).add(doc);
        }

        List<ReporteAprendizDto> resultado = new ArrayList<>();
        for (List<DocumentoSolicitud> docs : porSolicitud.values()) {
            SolicitudEtapaPractica solicitud = docs.get(0).getSolicitud();
            Usuario aprendiz = solicitud.getAprendizFicha().getUsuario();
            Ficha fichaAprendiz = solicitud.getAprendizFicha().getFicha();

            List<ReporteAprendizDto.DocumentoAprobado> documentos = docs.stream()
                    .sorted(Comparator.comparing(DocumentoSolicitud::getFechaSubida))
                    .map(d -> ReporteAprendizDto.DocumentoAprobado.builder()
                            .nombreDocumento(d.getPlantillaFormato() != null
                                    ? d.getPlantillaFormato().getNombreDocumento() : "Documento adjunto")
                            .asunto(d.getAsunto())
                            .fechaSubida(d.getFechaSubida().format(FORMATO_FECHA))
                            .rutaArchivo(d.getRutaArchivoLleno())
                            .build())
                    .toList();

            resultado.add(ReporteAprendizDto.builder()
                    .idSolicitud(solicitud.getIdSolicitud())
                    .nombre(aprendiz.getNombre())
                    .apellido(aprendiz.getApellido())
                    .tipoDocumento(aprendiz.getTipoDocumento() != null ? aprendiz.getTipoDocumento().name() : "")
                    .documento(aprendiz.getDocumento())
                    .numeroFicha(fichaAprendiz.getNumeroFicha())
                    .programa(fichaAprendiz.getProgramaFormacion().getNombrePrograma())
                    .modalidadContrato(solicitud.getSeccionFormato() != null
                            ? solicitud.getSeccionFormato().getNombreSeccion() : "—")
                    .estadoSolicitud(solicitud.getEstado().name())
                    .documentos(documentos)
                    .build());
        }

        resultado.sort(Comparator.comparing(ReporteAprendizDto::getApellido, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ReporteAprendizDto::getNombre, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim().toLowerCase();
    }

    // 📊 Genera el libro Excel (.xlsx): una fila por documento aprobado
    public byte[] generarExcel(List<ReporteAprendizDto> aprendices) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte Aprendiz");

            XSSFFont fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());

            CellStyle estiloTitulo = libro.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row filaTitulos = hoja.createRow(0);
            for (int i = 0; i < TITULOS.length; i++) {
                Cell celda = filaTitulos.createCell(i);
                celda.setCellValue(TITULOS[i]);
                celda.setCellStyle(estiloTitulo);
            }

            int numeroFila = 1;
            for (String[] valores : filas(aprendices)) {
                Row fila = hoja.createRow(numeroFila++);
                for (int i = 0; i < valores.length; i++) {
                    fila.createCell(i).setCellValue(valores[i]);
                }
            }

            for (int i = 0; i < TITULOS.length; i++) {
                hoja.autoSizeColumn(i);
            }

            libro.write(salida);
            return salida.toByteArray();
        }
    }

    // 📄 Genera el PDF apaisado: una fila por documento aprobado
    public byte[] generarPdf(List<ReporteAprendizDto> aprendices) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 16, 16, 22, 22);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        Paragraph titulo = new Paragraph("KRONOS - Reporte Aprendiz (Documentos Aprobados)",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(5, 112, 21)));
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph fechaGeneracion = new Paragraph("Generado el " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
        fechaGeneracion.setAlignment(Element.ALIGN_CENTER);
        fechaGeneracion.setSpacingAfter(12);
        documento.add(fechaGeneracion);

        PdfPTable tabla = new PdfPTable(TITULOS.length);
        tabla.setWidthPercentage(100);

        com.lowagie.text.Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, Color.WHITE);
        for (String tituloColumna : TITULOS) {
            PdfPCell celda = new PdfPCell(new Paragraph(tituloColumna, fuenteTitulo));
            celda.setBackgroundColor(new Color(5, 112, 21));
            celda.setPadding(3);
            tabla.addCell(celda);
        }

        com.lowagie.text.Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 7f);
        for (String[] valores : filas(aprendices)) {
            for (String valorCelda : valores) {
                PdfPCell celda = new PdfPCell(new Paragraph(valorCelda, fuenteCelda));
                celda.setPadding(2.5f);
                tabla.addCell(celda);
            }
        }

        documento.add(tabla);
        documento.close();
        return salida.toByteArray();
    }

    // Aplana cada aprendiz a una fila por documento aprobado (mismo orden que TITULOS)
    private List<String[]> filas(List<ReporteAprendizDto> aprendices) {
        List<String[]> filas = new ArrayList<>();
        for (ReporteAprendizDto a : aprendices) {
            for (ReporteAprendizDto.DocumentoAprobado doc : a.getDocumentos()) {
                filas.add(new String[]{
                        a.getTipoDocumento(), a.getDocumento(), a.getApellido(), a.getNombre(),
                        a.getNumeroFicha(), a.getPrograma(), a.getModalidadContrato(), a.getEstadoSolicitud(),
                        doc.getNombreDocumento(), (doc.getAsunto() == null || doc.getAsunto().isBlank()) ? "—" : doc.getAsunto(),
                        doc.getFechaSubida()
                });
            }
        }
        return filas;
    }
}
