package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.ResultadoImportacionAdmin;
import com.etapa_productiva.kronos.entity.*;
import com.etapa_productiva.kronos.repository.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 🗄️ Datos Maestros del Administrador: CRUD e importaciones masivas de
 * Áreas de Formación, Programas de Formación, Fichas y División Territorial (DIVIPOLA).
 * Cada operación deja rastro en AUDITORIA vía {@link AuditoriaService}.
 */
@Service
public class AdminDatosMaestrosService {

    private static final DateTimeFormatter[] FORMATOS_FECHA = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    @Autowired private AreasFormacionRepository areasFormacionRepository;
    @Autowired private CoordinacionAcademicaRepository coordinacionAcademicaRepository;
    @Autowired private ProgramasFormacionRepository programasFormacionRepository;
    @Autowired private NivelFormacionRepository nivelFormacionRepository;
    @Autowired private FichaRepository fichaRepository;
    @Autowired private DepartamentoRepository departamentoRepository;
    @Autowired private MunicipioRepository municipioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AuditoriaService auditoriaService;

    // ══════════════════════════ ÁREAS DE FORMACIÓN ══════════════════════════

    /**
     * Crea un área de formación. La coordinación es 1:1 con un Usuario, así que se
     * busca (o crea) la CoordinacionAcademica del usuario coordinador elegido.
     */
    @Transactional
    public AreasFormacion crearArea(String nombreArea, Long idUsuarioCoordinador, Long idAdmin) {
        if (nombreArea == null || nombreArea.isBlank()) {
            throw new IllegalArgumentException("El nombre del área de formación es obligatorio.");
        }
        if (areasFormacionRepository.findByNombreAreaFormacionIgnoreCase(nombreArea.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un área de formación llamada '" + nombreArea.trim() + "'.");
        }

        AreasFormacion area = areasFormacionRepository.save(AreasFormacion.builder()
                .nombreAreaFormacion(nombreArea.trim())
                .coordinacionAcademica(obtenerOCrearCoordinacion(idUsuarioCoordinador))
                .build());

        auditoriaService.registrar(idAdmin, AccionAuditoria.INSERT,
                "Área de formación creada: " + area.getNombreAreaFormacion());
        return area;
    }

    @Transactional
    public AreasFormacion editarArea(Long idArea, String nombreArea, Long idUsuarioCoordinador, Boolean estado, Long idAdmin) {
        AreasFormacion area = areasFormacionRepository.findById(idArea)
                .orElseThrow(() -> new IllegalArgumentException("El área de formación no existe."));

        if (nombreArea != null && !nombreArea.isBlank()) {
            area.setNombreAreaFormacion(nombreArea.trim());
        }
        if (idUsuarioCoordinador != null) {
            area.setCoordinacionAcademica(obtenerOCrearCoordinacion(idUsuarioCoordinador));
        }
        if (estado != null) {
            area.setEstado(estado);
        }

        area = areasFormacionRepository.save(area);
        auditoriaService.registrar(idAdmin, AccionAuditoria.UPDATE,
                "Área de formación actualizada: " + area.getNombreAreaFormacion());
        return area;
    }

    /**
     * 📥 Importa áreas desde Excel. Columnas: "AREA" (o "NOMBRE") obligatoria y
     * "COORDINADOR" opcional (correo o documento del usuario coordinador); si la fila
     * no trae coordinador se usa el usuario coordinador por defecto elegido en el formulario.
     */
    @Transactional
    public ResultadoImportacionAdmin importarAreas(MultipartFile archivo, Long idUsuarioCoordinadorPorDefecto, Long idAdmin) {
        validarExcel(archivo);
        int filas = 0, creados = 0, omitidos = 0;
        List<String> errores = new ArrayList<>();

        try (InputStream in = archivo.getInputStream(); Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            int filaCabecera = ubicarCabecera(hoja, fmt, "area", "nombre");
            if (filaCabecera < 0) {
                throw new IllegalArgumentException("No se encontró la columna 'AREA' (o 'NOMBRE') en el Excel.");
            }
            Row cab = hoja.getRow(filaCabecera);
            int colNombre = col(cab, fmt, "area", "nombre");
            int colCoordinador = col(cab, fmt, "coordinador", "coordinacion", "coordinación");

            for (int i = filaCabecera + 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                if (row == null) continue;
                String nombre = leer(row, colNombre, fmt);
                if (nombre.isBlank()) continue;
                filas++;

                try {
                    if (areasFormacionRepository.findByNombreAreaFormacionIgnoreCase(nombre).isPresent()) {
                        omitidos++;
                        continue;
                    }

                    Long idUsuarioCoordinador = idUsuarioCoordinadorPorDefecto;
                    String coordinadorTexto = leer(row, colCoordinador, fmt);
                    if (!coordinadorTexto.isBlank()) {
                        Usuario usuarioFila = buscarUsuarioPorCorreoODocumento(coordinadorTexto);
                        if (usuarioFila == null) {
                            errores.add("Fila " + (i + 1) + ": no existe un usuario con correo/documento '" + coordinadorTexto + "'.");
                            continue;
                        }
                        idUsuarioCoordinador = usuarioFila.getIdUsuario();
                    }
                    if (idUsuarioCoordinador == null) {
                        errores.add("Fila " + (i + 1) + ": la fila no trae coordinador y no se eligió uno por defecto.");
                        continue;
                    }

                    areasFormacionRepository.save(AreasFormacion.builder()
                            .nombreAreaFormacion(nombre)
                            .coordinacionAcademica(obtenerOCrearCoordinacion(idUsuarioCoordinador))
                            .build());
                    creados++;
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo Excel: " + e.getMessage(), e);
        }

        ResultadoImportacionAdmin resultado = new ResultadoImportacionAdmin(filas, creados, 0, omitidos, errores);
        auditoriaService.registrar(idAdmin, AccionAuditoria.IMPORTACION,
                "Importación de áreas de formación (" + archivo.getOriginalFilename() + "): " + resultado.resumen());
        return resultado;
    }

    private CoordinacionAcademica obtenerOCrearCoordinacion(Long idUsuario) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("Debes elegir el usuario coordinador del área.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario coordinador elegido no existe."));

        return coordinacionAcademicaRepository.findAll().stream()
                .filter(c -> c.getUsuario() != null && idUsuario.equals(c.getUsuario().getIdUsuario()))
                .findFirst()
                .orElseGet(() -> coordinacionAcademicaRepository.save(
                        CoordinacionAcademica.builder().usuario(usuario).build()));
    }

    private Usuario buscarUsuarioPorCorreoODocumento(String texto) {
        String limpio = texto.trim();
        return usuarioRepository.findByCorreoElectronico(limpio)
                .or(() -> usuarioRepository.findByDocumento(limpio))
                .orElse(null);
    }

    // ══════════════════════════ PROGRAMAS DE FORMACIÓN ══════════════════════════

    @Transactional
    public ProgramasFormacion crearPrograma(String nombrePrograma, Long idArea, Long idNivel, Long idAdmin) {
        if (nombrePrograma == null || nombrePrograma.isBlank()) {
            throw new IllegalArgumentException("El nombre del programa es obligatorio.");
        }
        if (programasFormacionRepository.findFirstByNombreProgramaIgnoreCase(nombrePrograma.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un programa llamado '" + nombrePrograma.trim() + "'.");
        }
        AreasFormacion area = areasFormacionRepository.findById(idArea)
                .orElseThrow(() -> new IllegalArgumentException("El área de formación elegida no existe."));
        NivelFormacion nivel = nivelFormacionRepository.findById(idNivel)
                .orElseThrow(() -> new IllegalArgumentException("El nivel de formación elegido no existe."));

        ProgramasFormacion programa = programasFormacionRepository.save(ProgramasFormacion.builder()
                .nombrePrograma(nombrePrograma.trim())
                .areaFormacion(area)
                .nivelFormacion(nivel)
                .build());

        auditoriaService.registrar(idAdmin, AccionAuditoria.INSERT,
                "Programa de formación creado: " + programa.getNombrePrograma()
                        + " (área " + area.getNombreAreaFormacion() + ", nivel " + nivel.getNombreNivel() + ")");
        return programa;
    }

    @Transactional
    public ProgramasFormacion editarPrograma(Long idPrograma, String nombrePrograma, Long idArea, Long idNivel, Boolean estado, Long idAdmin) {
        ProgramasFormacion programa = programasFormacionRepository.findById(idPrograma)
                .orElseThrow(() -> new IllegalArgumentException("El programa de formación no existe."));

        if (nombrePrograma != null && !nombrePrograma.isBlank()) {
            programa.setNombrePrograma(nombrePrograma.trim());
        }
        if (idArea != null) {
            programa.setAreaFormacion(areasFormacionRepository.findById(idArea)
                    .orElseThrow(() -> new IllegalArgumentException("El área de formación elegida no existe.")));
        }
        if (idNivel != null) {
            programa.setNivelFormacion(nivelFormacionRepository.findById(idNivel)
                    .orElseThrow(() -> new IllegalArgumentException("El nivel de formación elegido no existe.")));
        }
        if (estado != null) {
            programa.setEstado(estado);
        }

        programa = programasFormacionRepository.save(programa);
        auditoriaService.registrar(idAdmin, AccionAuditoria.UPDATE,
                "Programa de formación actualizado: " + programa.getNombrePrograma());
        return programa;
    }

    // ══════════════════════════ FICHAS ══════════════════════════

    @Transactional
    public Ficha crearFicha(String numeroFicha, Long idPrograma, Jornada jornada,
                            LocalDate fechaInicio, LocalDate fechaFin, Long idAdmin) {
        if (numeroFicha == null || numeroFicha.isBlank()) {
            throw new IllegalArgumentException("El número de ficha es obligatorio.");
        }
        if (fichaRepository.findByNumeroFicha(numeroFicha.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe la ficha " + numeroFicha.trim() + ".");
        }
        if (fechaInicio == null || fechaFin == null || fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("Las fechas de inicio y fin de la ficha son obligatorias y deben ser coherentes.");
        }
        ProgramasFormacion programa = programasFormacionRepository.findById(idPrograma)
                .orElseThrow(() -> new IllegalArgumentException("El programa de formación elegido no existe."));

        Ficha ficha = fichaRepository.save(Ficha.builder()
                .numeroFicha(numeroFicha.trim())
                .programaFormacion(programa)
                .jornada(jornada)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .build());

        auditoriaService.registrar(idAdmin, AccionAuditoria.INSERT,
                "Ficha creada: " + ficha.getNumeroFicha() + " (" + programa.getNombrePrograma() + ")");
        return ficha;
    }

    @Transactional
    public Ficha editarFicha(Long idFicha, Long idPrograma, Jornada jornada,
                             LocalDate fechaInicio, LocalDate fechaFin, Boolean estado, Long idAdmin) {
        Ficha ficha = fichaRepository.findById(idFicha)
                .orElseThrow(() -> new IllegalArgumentException("La ficha no existe."));

        if (idPrograma != null) {
            ficha.setProgramaFormacion(programasFormacionRepository.findById(idPrograma)
                    .orElseThrow(() -> new IllegalArgumentException("El programa de formación elegido no existe.")));
        }
        if (jornada != null) ficha.setJornada(jornada);
        if (fechaInicio != null) ficha.setFechaInicio(fechaInicio);
        if (fechaFin != null) ficha.setFechaFin(fechaFin);
        if (estado != null) ficha.setEstado(estado);

        ficha = fichaRepository.save(ficha);
        auditoriaService.registrar(idAdmin, AccionAuditoria.UPDATE, "Ficha actualizada: " + ficha.getNumeroFicha());
        return ficha;
    }

    /**
     * 📥 Importa fichas desde Excel. Columnas: "FICHA" obligatoria; "PROGRAMA" (debe existir
     * en el catálogo, si falta se usa el programa por defecto del formulario); "JORNADA",
     * "INICIO" y "FIN" opcionales (si faltan se usan las fechas por defecto del formulario).
     */
    @Transactional
    public ResultadoImportacionAdmin importarFichas(MultipartFile archivo, Long idProgramaPorDefecto,
                                                     LocalDate inicioPorDefecto, LocalDate finPorDefecto, Long idAdmin) {
        validarExcel(archivo);
        int filas = 0, creados = 0, actualizados = 0, omitidos = 0;
        List<String> errores = new ArrayList<>();

        try (InputStream in = archivo.getInputStream(); Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            int filaCabecera = ubicarCabecera(hoja, fmt, "ficha");
            if (filaCabecera < 0) {
                throw new IllegalArgumentException("No se encontró la columna 'FICHA' en el Excel.");
            }
            Row cab = hoja.getRow(filaCabecera);
            int colFicha = col(cab, fmt, "ficha");
            int colPrograma = col(cab, fmt, "programa", "formacion", "formación");
            int colJornada = col(cab, fmt, "jornada");
            int colInicio = col(cab, fmt, "inicio");
            int colFin = col(cab, fmt, "fin", "terminacion", "terminación");

            for (int i = filaCabecera + 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                if (row == null) continue;
                String numero = leer(row, colFicha, fmt);
                if (numero.isBlank() || numero.chars().noneMatch(Character::isDigit)) continue;
                filas++;

                try {
                    // Programa: el de la fila si viene, o el elegido por defecto en el formulario
                    ProgramasFormacion programa = null;
                    String nombrePrograma = leer(row, colPrograma, fmt);
                    if (!nombrePrograma.isBlank()) {
                        programa = programasFormacionRepository.findFirstByNombreProgramaIgnoreCase(nombrePrograma).orElse(null);
                        if (programa == null) {
                            errores.add("Fila " + (i + 1) + ": el programa '" + nombrePrograma + "' no existe en el catálogo.");
                            continue;
                        }
                    } else if (idProgramaPorDefecto != null) {
                        programa = programasFormacionRepository.findById(idProgramaPorDefecto).orElse(null);
                    }
                    if (programa == null) {
                        errores.add("Fila " + (i + 1) + ": la fila no trae programa y no se eligió uno por defecto.");
                        continue;
                    }

                    Jornada jornada = parsearJornada(leer(row, colJornada, fmt));
                    LocalDate inicio = parsearFecha(leer(row, colInicio, fmt), inicioPorDefecto);
                    LocalDate fin = parsearFecha(leer(row, colFin, fmt), finPorDefecto);
                    if (inicio == null || fin == null) {
                        errores.add("Fila " + (i + 1) + ": la ficha no trae fechas y no se indicaron fechas por defecto.");
                        continue;
                    }

                    Optional<Ficha> existente = fichaRepository.findByNumeroFicha(numero.trim());
                    if (existente.isPresent()) {
                        // La ficha ya existe: solo se completa la jornada si estaba vacía
                        Ficha ficha = existente.get();
                        if (ficha.getJornada() == null && jornada != null) {
                            ficha.setJornada(jornada);
                            fichaRepository.save(ficha);
                            actualizados++;
                        } else {
                            omitidos++;
                        }
                        continue;
                    }

                    fichaRepository.save(Ficha.builder()
                            .numeroFicha(numero.trim())
                            .programaFormacion(programa)
                            .jornada(jornada)
                            .fechaInicio(inicio)
                            .fechaFin(fin)
                            .build());
                    creados++;
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo Excel: " + e.getMessage(), e);
        }

        ResultadoImportacionAdmin resultado = new ResultadoImportacionAdmin(filas, creados, actualizados, omitidos, errores);
        auditoriaService.registrar(idAdmin, AccionAuditoria.IMPORTACION,
                "Importación de fichas (" + archivo.getOriginalFilename() + "): " + resultado.resumen());
        return resultado;
    }

    // ══════════════════════════ DIVIPOLA ══════════════════════════

    @Transactional
    public Departamento crearDepartamento(String nombre, Long idAdmin) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del departamento es obligatorio.");
        }
        if (departamentoRepository.findByNombreDepartamentoIgnoreCase(nombre.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe el departamento '" + nombre.trim() + "'.");
        }
        Departamento departamento = departamentoRepository.save(
                Departamento.builder().nombreDepartamento(nombre.trim()).build());
        auditoriaService.registrar(idAdmin, AccionAuditoria.INSERT, "Departamento creado: " + departamento.getNombreDepartamento());
        return departamento;
    }

    @Transactional
    public Municipio crearMunicipio(String nombre, Long idDepartamento, Long idAdmin) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del municipio es obligatorio.");
        }
        Departamento departamento = departamentoRepository.findById(idDepartamento)
                .orElseThrow(() -> new IllegalArgumentException("El departamento elegido no existe."));
        if (municipioRepository.findByNombreMunicipioIgnoreCaseAndDepartamentoIdDepartamento(nombre.trim(), idDepartamento).isPresent()) {
            throw new IllegalArgumentException("El municipio '" + nombre.trim() + "' ya existe en " + departamento.getNombreDepartamento() + ".");
        }
        Municipio municipio = municipioRepository.save(Municipio.builder()
                .nombreMunicipio(nombre.trim())
                .departamento(departamento)
                .build());
        auditoriaService.registrar(idAdmin, AccionAuditoria.INSERT,
                "Municipio creado: " + municipio.getNombreMunicipio() + " (" + departamento.getNombreDepartamento() + ")");
        return municipio;
    }

    @Transactional
    public void actualizarNombreDepartamento(Long idDepartamento, String nombre, Long idAdmin) {
        Departamento departamento = departamentoRepository.findById(idDepartamento)
                .orElseThrow(() -> new IllegalArgumentException("El departamento no existe."));
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del departamento es obligatorio.");
        }
        String anterior = departamento.getNombreDepartamento();
        departamento.setNombreDepartamento(nombre.trim());
        departamentoRepository.save(departamento);
        auditoriaService.registrar(idAdmin, AccionAuditoria.UPDATE,
                "Departamento renombrado: " + anterior + " → " + nombre.trim());
    }

    @Transactional
    public void actualizarNombreMunicipio(Long idMunicipio, String nombre, Long idAdmin) {
        Municipio municipio = municipioRepository.findById(idMunicipio)
                .orElseThrow(() -> new IllegalArgumentException("El municipio no existe."));
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del municipio es obligatorio.");
        }
        String anterior = municipio.getNombreMunicipio();
        municipio.setNombreMunicipio(nombre.trim());
        municipioRepository.save(municipio);
        auditoriaService.registrar(idAdmin, AccionAuditoria.UPDATE,
                "Municipio renombrado: " + anterior + " → " + nombre.trim());
    }

    /**
     * 📥 Importa la División Territorial desde Excel. Columnas: "DEPARTAMENTO" y "MUNICIPIO".
     * Crea el departamento si no existe y luego el municipio bajo él (sin duplicar ninguno).
     */
    @Transactional
    public ResultadoImportacionAdmin importarDivipola(MultipartFile archivo, Long idAdmin) {
        validarExcel(archivo);
        int filas = 0, creados = 0, omitidos = 0;
        List<String> errores = new ArrayList<>();

        try (InputStream in = archivo.getInputStream(); Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            int filaCabecera = ubicarCabecera(hoja, fmt, "departamento");
            if (filaCabecera < 0) {
                throw new IllegalArgumentException("No se encontró la columna 'DEPARTAMENTO' en el Excel.");
            }
            Row cab = hoja.getRow(filaCabecera);
            int colDepartamento = col(cab, fmt, "departamento");
            int colMunicipio = col(cab, fmt, "municipio");

            for (int i = filaCabecera + 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                if (row == null) continue;
                String nombreDepartamento = leer(row, colDepartamento, fmt);
                String nombreMunicipio = leer(row, colMunicipio, fmt);
                if (nombreDepartamento.isBlank() && nombreMunicipio.isBlank()) continue;
                filas++;

                try {
                    if (nombreDepartamento.isBlank()) {
                        errores.add("Fila " + (i + 1) + ": el municipio '" + nombreMunicipio + "' no trae departamento.");
                        continue;
                    }

                    Departamento departamento = departamentoRepository
                            .findByNombreDepartamentoIgnoreCase(nombreDepartamento)
                            .orElseGet(() -> departamentoRepository.save(
                                    Departamento.builder().nombreDepartamento(nombreDepartamento.trim()).build()));

                    if (nombreMunicipio.isBlank()) {
                        omitidos++; // fila solo de departamento: ya quedó creado/reusado
                        continue;
                    }

                    boolean existe = municipioRepository
                            .findByNombreMunicipioIgnoreCaseAndDepartamentoIdDepartamento(
                                    nombreMunicipio, departamento.getIdDepartamento())
                            .isPresent();
                    if (existe) {
                        omitidos++;
                        continue;
                    }

                    municipioRepository.save(Municipio.builder()
                            .nombreMunicipio(nombreMunicipio.trim())
                            .departamento(departamento)
                            .build());
                    creados++;
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo Excel: " + e.getMessage(), e);
        }

        ResultadoImportacionAdmin resultado = new ResultadoImportacionAdmin(filas, creados, 0, omitidos, errores);
        auditoriaService.registrar(idAdmin, AccionAuditoria.IMPORTACION,
                "Importación DIVIPOLA (" + archivo.getOriginalFilename() + "): " + resultado.resumen());
        return resultado;
    }

    // ══════════════════════════ Helpers de lectura del Excel ══════════════════════════

    private void validarExcel(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar el archivo Excel a importar.");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!nombre.endsWith(".xlsx") && !nombre.endsWith(".xls")) {
            throw new IllegalArgumentException("El archivo a importar debe ser un Excel (.xlsx o .xls).");
        }
    }

    private Jornada parsearJornada(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String limpio = texto.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
                .replace(" ", "_");
        for (Jornada jornada : Jornada.values()) {
            if (jornada.name().equals(limpio) || limpio.contains(jornada.name())) return jornada;
        }
        if (limpio.contains("FIN")) return Jornada.FINES_DE_SEMANA;
        return null;
    }

    private LocalDate parsearFecha(String texto, LocalDate porDefecto) {
        if (texto == null || texto.isBlank()) return porDefecto;
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(texto.trim(), formato);
            } catch (Exception ignorada) {
                // se intenta el siguiente formato
            }
        }
        return porDefecto;
    }

    private int ubicarCabecera(Sheet hoja, DataFormatter fmt, String... aliasObligatorios) {
        int limite = Math.min(hoja.getLastRowNum(), 8);
        for (int i = hoja.getFirstRowNum(); i <= limite; i++) {
            Row row = hoja.getRow(i);
            if (row != null && col(row, fmt, aliasObligatorios) >= 0) return i;
        }
        return -1;
    }

    private int col(Row cabecera, DataFormatter fmt, String... alias) {
        if (cabecera == null) return -1;
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
