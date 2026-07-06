package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.DatosFilaVisitaExcel;
import com.etapa_productiva.kronos.dto.FilaImportadaVisitaInfo;
import com.etapa_productiva.kronos.dto.ResultadoImportacionVisitas;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 📥 Orquesta la importación masiva del Excel institucional de "Visitas de Seguimiento":
 * detecta las columnas por encabezado, valida el archivo, y delega cada fila a
 * {@link ImportacionVisitaFilaService} (que la procesa en su propia transacción, así que un
 * error en una fila no afecta a las demás ya importadas con éxito).
 *
 * Filas cuyo documento está vacío, dice "NO" o no contiene ningún dígito se consideran
 * relleno de la plantilla y se saltan en silencio (no cuentan como fila ni como error).
 */
@Service
public class ImportacionVisitasSeguimientoService {

    @Autowired
    private ImportacionVisitaFilaService importacionVisitaFilaService;

    public ResultadoImportacionVisitas importar(MultipartFile archivo, Long idUsuarioInstructor,
                                                 LocalDate fichaInicioGlobal, LocalDate fichaFinGlobal,
                                                 String tipoContratoRespaldo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar el archivo Excel a importar.");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!nombre.endsWith(".xlsx") && !nombre.endsWith(".xls")) {
            throw new IllegalArgumentException("El archivo a importar debe ser un Excel (.xlsx o .xls).");
        }
        if (fichaInicioGlobal == null || fichaFinGlobal == null) {
            throw new IllegalArgumentException("Debes indicar la fecha de inicio y fin de ficha antes de importar.");
        }

        int filas = 0, usuariosCreados = 0, fichasCreadas = 0, programasCreados = 0,
                empresasCreadas = 0, tiposContratoCreados = 0, etapasCreadas = 0, visitasCreadas = 0, omitidas = 0;
        List<String> errores = new ArrayList<>();

        try (InputStream in = archivo.getInputStream(); Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            int filaCabecera = ubicarCabecera(hoja, fmt);
            if (filaCabecera < 0) {
                throw new IllegalArgumentException("No se encontró la fila de encabezados (se esperan columnas como 'Nombre Aprendiz', 'No. Doc', 'Ficha').");
            }

            Row cab = hoja.getRow(filaCabecera);
            int colNombre = col(cab, fmt, "nombre aprendiz", "nombre");
            int colTipoDoc = colTipoDocumento(cab, fmt);
            int colDocumento = col(cab, fmt, "no. doc", "no doc", "documento", "cedula", "cédula");
            int colFicha = col(cab, fmt, "ficha");
            int colNivel = col(cab, fmt, "nivel");
            int colPrograma = col(cab, fmt, "formacion", "formación");
            int colTipoContrato = col(cab, fmt, "alternativa", "contrato");
            int colFechaTerminacion = col(cab, fmt, "terminacion", "terminación");
            int colNit = col(cab, fmt, "nit");
            int colRazonSocial = col(cab, fmt, "razon social", "razón social");
            int colDireccion = col(cab, fmt, "direccion", "dirección");
            int colMunicipio = col(cab, fmt, "municipio");
            int colJefeNombre = col(cab, fmt, "nombre jefe", "jefe inmediato");
            int colJefeCorreo = col(cab, fmt, "correo");
            int colJefeTelefono = col(cab, fmt, "telefono", "teléfono");
            int colModalidadVisita = col(cab, fmt, "modalidad");
            int colTipoVisita = col(cab, fmt, "tipo de visita", "tipo visita");
            int colFechaVisita = col(cab, fmt, "fecha visita");
            int colActa = col(cab, fmt, "acta");

            if (colDocumento < 0 || colNombre < 0 || colFicha < 0) {
                throw new IllegalArgumentException("Faltan columnas obligatorias: se requieren al menos 'Nombre Aprendiz', 'No. Doc' y 'Ficha'.");
            }

            for (int i = filaCabecera + 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                if (row == null) continue;

                String documento = leer(row, colDocumento, fmt);
                if (esFilaVacia(documento)) continue; // relleno de la plantilla ("NO", vacío, sin dígitos)
                filas++;

                try {
                    DatosFilaVisitaExcel datos = new DatosFilaVisitaExcel(
                            documento,
                            leer(row, colTipoDoc, fmt),
                            leer(row, colNombre, fmt),
                            leer(row, colFicha, fmt),
                            leer(row, colNivel, fmt),
                            leer(row, colPrograma, fmt),
                            leer(row, colTipoContrato, fmt),
                            leer(row, colFechaTerminacion, fmt),
                            leer(row, colNit, fmt),
                            leer(row, colRazonSocial, fmt),
                            leer(row, colDireccion, fmt),
                            leer(row, colMunicipio, fmt),
                            leer(row, colJefeNombre, fmt),
                            leer(row, colJefeCorreo, fmt),
                            leer(row, colJefeTelefono, fmt),
                            leer(row, colModalidadVisita, fmt),
                            leer(row, colTipoVisita, fmt),
                            leer(row, colFechaVisita, fmt),
                            leer(row, colActa, fmt)
                    );

                    FilaImportadaVisitaInfo info = importacionVisitaFilaService.procesarFila(
                            datos, idUsuarioInstructor, fichaInicioGlobal, fichaFinGlobal, tipoContratoRespaldo);

                    if (info.omitida()) {
                        omitidas++;
                        errores.add("Fila " + (i + 1) + ": el aprendiz ya tiene una Etapa Productiva activa, se omitió.");
                        continue;
                    }
                    if (info.usuarioCreado()) usuariosCreados++;
                    if (info.fichaCreada()) fichasCreadas++;
                    if (info.programaCreado()) programasCreados++;
                    if (info.empresaCreada()) empresasCreadas++;
                    if (info.tipoContratoCreado()) tiposContratoCreados++;
                    if (info.etapaCreada()) etapasCreadas++;
                    if (info.visitaCreada()) visitasCreadas++;
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo Excel: " + e.getMessage(), e);
        }

        return new ResultadoImportacionVisitas(filas, usuariosCreados, fichasCreadas, programasCreados,
                empresasCreadas, tiposContratoCreados, etapasCreadas, visitasCreadas, omitidas, errores);
    }

    // ─────────────────────────────── Helpers de lectura del Excel ───────────────────────────────

    private boolean esFilaVacia(String documento) {
        if (documento == null || documento.isBlank()) return true;
        String t = documento.trim();
        if (t.equalsIgnoreCase("NO") || t.equalsIgnoreCase("N/A") || t.equals("-")) return true;
        return t.chars().noneMatch(Character::isDigit);
    }

    private int ubicarCabecera(Sheet hoja, DataFormatter fmt) {
        int limite = Math.min(hoja.getLastRowNum(), 8);
        for (int i = hoja.getFirstRowNum(); i <= limite; i++) {
            Row row = hoja.getRow(i);
            if (row == null) continue;
            boolean tieneDoc = col(row, fmt, "no. doc", "no doc", "documento", "cedula", "cédula") >= 0;
            boolean tieneNombre = col(row, fmt, "nombre") >= 0;
            if (tieneDoc && tieneNombre) return i;
        }
        return -1;
    }

    // "TP" (tipo de documento) es un encabezado muy corto: exige coincidencia exacta o "tipo doc",
    // para no confundirlo con alguna otra columna que por casualidad contenga esas dos letras.
    private int colTipoDocumento(Row cabecera, DataFormatter fmt) {
        for (Cell celda : cabecera) {
            String texto = fmt.formatCellValue(celda).toLowerCase(Locale.ROOT).trim();
            if (texto.equals("tp") || (texto.contains("tipo") && texto.contains("doc"))) {
                return celda.getColumnIndex();
            }
        }
        return -1;
    }

    private int col(Row cabecera, DataFormatter fmt, String... alias) {
        for (Cell celda : cabecera) {
            String texto = fmt.formatCellValue(celda).toLowerCase(Locale.ROOT).trim();
            for (String a : alias) {
                if (texto.contains(a.toLowerCase(Locale.ROOT))) return celda.getColumnIndex();
            }
        }
        return -1;
    }

    private String leer(Row row, int col, DataFormatter fmt) {
        if (col < 0) return "";
        Cell celda = row.getCell(col);
        return celda == null ? "" : fmt.formatCellValue(celda).trim();
    }
}
