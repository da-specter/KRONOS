package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.DashboardInstructorDto.BarraGrafico;
import com.etapa_productiva.kronos.dto.DashboardInstructorDto.SegmentoGrafico;
import com.etapa_productiva.kronos.dto.DashboardTecnicoDto;
import com.etapa_productiva.kronos.dto.TecnicoAprendizDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.EstadoAcademico;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.InstructorTecnico;
import com.etapa_productiva.kronos.entity.InstructorTecnicoFicha;
import com.etapa_productiva.kronos.entity.Rol;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.UsuarioRol;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.InstructorTecnicoFichaRepository;
import com.etapa_productiva.kronos.repository.InstructorTecnicoRepository;
import com.etapa_productiva.kronos.repository.RolRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 🛠️ Módulo del Instructor Técnico (líder de ficha): consolida los aprendices de las fichas
 * que tiene asignadas (solo consulta de su etapa práctica), arma su dashboard (en etapa,
 * sin etapa, por certificar) y le permite añadir aprendices a sus fichas o importarlos por Excel.
 */
@Service
public class InstructorTecnicoService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SIN_DATO = "—";

    // Situaciones del aprendiz y su color para las gráficas (orden fijo para la leyenda)
    public static final String SIT_EN_ETAPA = "En etapa práctica";
    public static final String SIT_SIN_ETAPA = "Sin etapa práctica";
    public static final String SIT_POR_CERTIFICAR = "Por certificar";
    public static final String SIT_CERTIFICADO = "Certificado";

    private static final LinkedHashMap<String, String> COLOR_SITUACION = new LinkedHashMap<>();
    static {
        COLOR_SITUACION.put(SIT_EN_ETAPA, "#057015");
        COLOR_SITUACION.put(SIT_SIN_ETAPA, "#D97706");
        COLOR_SITUACION.put(SIT_POR_CERTIFICAR, "#2563EB");
        COLOR_SITUACION.put(SIT_CERTIFICADO, "#6B7280");
    }

    @Autowired
    private InstructorTecnicoRepository instructorTecnicoRepository;

    @Autowired
    private InstructorTecnicoFichaRepository instructorTecnicoFichaRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─────────────────────────────── Consulta ───────────────────────────────

    /** Fichas activas asignadas al instructor técnico (resuelto desde el usuario logueado). */
    @Transactional(readOnly = true)
    public List<InstructorTecnicoFicha> listarFichas(Long idUsuario) {
        return instructorTecnicoRepository.findByUsuarioIdUsuario(idUsuario)
                .map(InstructorTecnico::getIdInstructorTecnico)
                .map(instructorTecnicoFichaRepository::findByInstructorTecnicoIdInstructorTecnicoAndEstadoTrue)
                .orElse(new ArrayList<>());
    }

    /** Todos los aprendices matriculados en las fichas del instructor, con su etapa práctica (si existe). */
    @Transactional(readOnly = true)
    public List<TecnicoAprendizDto> listarAprendices(Long idUsuario) {
        List<TecnicoAprendizDto> filas = new ArrayList<>();

        for (InstructorTecnicoFicha asignacion : listarFichas(idUsuario)) {
            Ficha ficha = asignacion.getFicha();

            // Etapa productiva de cada matrícula de la ficha (una consulta por ficha)
            Map<Long, EtapaProductiva> etapaPorMatricula = new HashMap<>();
            for (EtapaProductiva etapa : etapaProductivaRepository.findByAprendizFichaFichaIdFicha(ficha.getIdFicha())) {
                etapaPorMatricula.put(etapa.getAprendizFicha().getIdAprendizFicha(), etapa);
            }

            for (AprendizFicha matricula : aprendizFichaRepository.findByFichaIdFicha(ficha.getIdFicha())) {
                Usuario aprendiz = matricula.getUsuario();
                EtapaProductiva etapa = etapaPorMatricula.get(matricula.getIdAprendizFicha());

                filas.add(TecnicoAprendizDto.builder()
                        .idAprendizFicha(matricula.getIdAprendizFicha())
                        .nombres(valor(aprendiz.getNombre()))
                        .apellidos(valor(aprendiz.getApellido()))
                        .tipoDocumento(aprendiz.getTipoDocumento() != null ? aprendiz.getTipoDocumento().name() : SIN_DATO)
                        .documento(valor(aprendiz.getDocumento()))
                        .telefono(valor(aprendiz.getTelefono()))
                        .correoElectronico(valor(aprendiz.getCorreoElectronico()))
                        .ficha(ficha.getNumeroFicha())
                        .programaFormacion(ficha.getProgramaFormacion().getNombrePrograma())
                        .estadoAcademico(etiquetaEstadoAcademico(matricula.getEstadoAcademico()))
                        .situacion(clasificarSituacion(matricula, etapa))
                        .empresa(etapa != null ? etapa.getEmpresa().getNombreEmpresa() : SIN_DATO)
                        .modalidadContrato(etapa != null ? etapa.getTipoContrato().getNombreTipoContrato() : SIN_DATO)
                        .modalidad(etapa != null && etapa.getModalidad() != null ? etiquetaModalidad(etapa.getModalidad().name()) : SIN_DATO)
                        .etapaInicio(etapa != null ? fecha(etapa.getFechaInicio()) : SIN_DATO)
                        .etapaFin(etapa != null ? fecha(etapa.getFechaFin()) : SIN_DATO)
                        .estadoEtapa(etapa != null && etapa.getEstadoEtapa() != null ? etapa.getEstadoEtapa().name() : SIN_DATO)
                        .build());
            }
        }

        filas.sort(Comparator.comparing(TecnicoAprendizDto::getFicha)
                .thenComparing(TecnicoAprendizDto::getApellidos, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TecnicoAprendizDto::getNombres, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    // La situación consolida el estado académico de la matrícula con la existencia de la etapa:
    // certificado > por certificar > tiene etapa registrada > aún sin etapa práctica.
    private String clasificarSituacion(AprendizFicha matricula, EtapaProductiva etapa) {
        if (matricula.getEstadoAcademico() == EstadoAcademico.CERTIFICADO) return SIT_CERTIFICADO;
        if (matricula.getEstadoAcademico() == EstadoAcademico.POR_CERTIFICAR) return SIT_POR_CERTIFICAR;
        return etapa != null ? SIT_EN_ETAPA : SIT_SIN_ETAPA;
    }

    /** Calcula números y series de gráficas a partir de la lista de aprendices. */
    public DashboardTecnicoDto calcularDashboard(List<TecnicoAprendizDto> aprendices) {
        Map<String, Integer> porSituacion = new LinkedHashMap<>();
        COLOR_SITUACION.keySet().forEach(s -> porSituacion.put(s, 0));
        Map<String, Integer> porFicha = new LinkedHashMap<>();

        for (TecnicoAprendizDto a : aprendices) {
            porSituacion.merge(a.getSituacion(), 1, Integer::sum);
            porFicha.merge(a.getFicha(), 1, Integer::sum);
        }

        int total = aprendices.size();

        // Segmentos de la dona (solo situaciones con al menos 1) + gradiente conic
        List<SegmentoGrafico> segmentos = new ArrayList<>();
        StringBuilder gradiente = new StringBuilder("conic-gradient(");
        double gradoActual = 0;
        boolean primero = true;
        for (Map.Entry<String, String> e : COLOR_SITUACION.entrySet()) {
            int cant = porSituacion.getOrDefault(e.getKey(), 0);
            if (cant == 0) continue;
            int pct = total > 0 ? (int) Math.round(cant * 100.0 / total) : 0;
            segmentos.add(SegmentoGrafico.builder()
                    .etiqueta(e.getKey()).cantidad(cant).porcentaje(pct).color(e.getValue()).build());

            double grados = total > 0 ? cant * 360.0 / total : 0;
            if (!primero) gradiente.append(", ");
            gradiente.append(e.getValue()).append(" ").append(Math.round(gradoActual)).append("deg ")
                    .append(Math.round(gradoActual + grados)).append("deg");
            gradoActual += grados;
            primero = false;
        }
        if (total == 0) {
            gradiente.append("#E5E7EB 0deg 360deg");
        }
        gradiente.append(")");

        // Barras: aprendices por ficha (ancho relativo al máximo)
        int maxFicha = porFicha.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        List<BarraGrafico> barras = new ArrayList<>();
        porFicha.forEach((ficha, cant) -> barras.add(BarraGrafico.builder()
                .etiqueta(ficha).cantidad(cant).porcentaje((int) Math.round(cant * 100.0 / maxFicha)).build()));
        barras.sort(Comparator.comparingInt(BarraGrafico::getCantidad).reversed());

        return DashboardTecnicoDto.builder()
                .totalAprendices(total)
                .enEtapa(porSituacion.getOrDefault(SIT_EN_ETAPA, 0))
                .sinEtapa(porSituacion.getOrDefault(SIT_SIN_ETAPA, 0))
                .porCertificar(porSituacion.getOrDefault(SIT_POR_CERTIFICAR, 0))
                .certificados(porSituacion.getOrDefault(SIT_CERTIFICADO, 0))
                .totalFichas(porFicha.size())
                .donutGradient(gradiente.toString())
                .segmentosSituacion(segmentos)
                .aprendicesPorFicha(barras)
                .build();
    }

    // ─────────────────────────────── Exportación ───────────────────────────────

    private static final String[] TITULOS_EXPORTACION = {
            "Nombres", "Apellidos", "Tipo Doc.", "Documento", "Teléfono", "Correo",
            "Ficha", "Programa", "Estado Académico", "Situación", "Empresa",
            "Modalidad Contrato", "Modalidad", "Inicio Etapa", "Fin Etapa", "Estado Etapa"
    };

    private List<String[]> filasExportacion(List<TecnicoAprendizDto> aprendices) {
        List<String[]> filas = new ArrayList<>();
        for (TecnicoAprendizDto a : aprendices) {
            filas.add(new String[]{
                    a.getNombres(), a.getApellidos(), a.getTipoDocumento(), a.getDocumento(),
                    a.getTelefono(), a.getCorreoElectronico(), a.getFicha(), a.getProgramaFormacion(),
                    a.getEstadoAcademico(), a.getSituacion(), a.getEmpresa(),
                    a.getModalidadContrato(), a.getModalidad(), a.getEtapaInicio(), a.getEtapaFin(), a.getEstadoEtapa()
            });
        }
        return filas;
    }

    /** 📊 Libro Excel (.xlsx) con los aprendices de las fichas del instructor. */
    public byte[] generarExcel(List<TecnicoAprendizDto> aprendices) throws IOException {
        return ExportacionUtil.excel("Mis Aprendices", TITULOS_EXPORTACION, filasExportacion(aprendices));
    }

    /** 📄 PDF apaisado con los aprendices de las fichas del instructor. */
    public byte[] generarPdf(List<TecnicoAprendizDto> aprendices) {
        return ExportacionUtil.pdf("KRONOS - Aprendices de Mis Fichas (Instructor Técnico)",
                TITULOS_EXPORTACION, filasExportacion(aprendices));
    }

    // ─────────────────────────────── Añadir aprendiz ───────────────────────────────

    /**
     * ➕ Matricula un aprendiz en una de las fichas del instructor. Si el documento no existe
     * crea el Usuario con rol APRENDIZ (contraseña inicial = documento). Valida que la ficha
     * pertenezca al instructor y que el aprendiz no esté ya matriculado en otra ficha.
     */
    @Transactional
    public String agregarAprendiz(Long idUsuarioInstructor, Long idFicha, String tipoDocumento,
                                  String documento, String nombre, String apellido,
                                  String telefono, String correo) {
        Ficha ficha = fichaDelInstructor(idUsuarioInstructor, idFicha);

        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El número de documento del aprendiz es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del aprendiz es obligatorio.");
        }

        boolean[] usuarioNuevo = {false};
        Usuario aprendiz = obtenerOCrearAprendiz(documento.trim(), tipoDocumento, nombre, apellido, telefono, correo, usuarioNuevo);

        matricular(aprendiz, ficha);

        return usuarioNuevo[0]
                ? "Aprendiz " + aprendiz.getNombre() + " " + aprendiz.getApellido()
                    + " creado y matriculado en la ficha " + ficha.getNumeroFicha()
                    + ". Su contraseña inicial es su número de documento."
                : "Aprendiz " + aprendiz.getNombre() + " " + aprendiz.getApellido()
                    + " matriculado en la ficha " + ficha.getNumeroFicha() + ".";
    }

    // ─────────────────────────────── Importación ───────────────────────────────

    /** Resultado resumido de una importación de aprendices a una ficha del instructor. */
    public record ResultadoImportacionTecnico(int filas, int aprendicesCreados, int matriculasCreadas,
                                              List<String> errores) {
    }

    /**
     * 📤 Importa un Excel de aprendices (Nombre, Apellido, Tipo Documento, Número Documento,
     * Teléfono) y los matricula en la ficha seleccionada, que debe pertenecer al instructor.
     */
    @Transactional
    public ResultadoImportacionTecnico importarAprendices(Long idUsuarioInstructor, Long idFicha, org.springframework.web.multipart.MultipartFile archivo) {
        Ficha ficha = fichaDelInstructor(idUsuarioInstructor, idFicha);

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar el archivo Excel a importar.");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!nombre.endsWith(".xlsx") && !nombre.endsWith(".xls")) {
            throw new IllegalArgumentException("El archivo a importar debe ser un Excel (.xlsx o .xls).");
        }

        int filas = 0, aprendicesCreados = 0, matriculasCreadas = 0;
        List<String> errores = new ArrayList<>();

        try (InputStream in = archivo.getInputStream(); Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            int filaCabecera = ubicarCabecera(hoja, fmt);
            if (filaCabecera < 0) {
                throw new IllegalArgumentException("No se encontró la fila de encabezados (se esperan columnas como 'Nombre' y 'Documento').");
            }

            Row cab = hoja.getRow(filaCabecera);
            int colNombre = col(cab, fmt, "nombre");
            int colApellido = col(cab, fmt, "apellido");
            int colTipoDoc = colTipoDocumento(cab, fmt);
            int colNumDoc = colNumeroDocumento(cab, fmt);
            int colTelefono = col(cab, fmt, "tel", "teléfono", "telefono", "celular");
            int colCorreo = col(cab, fmt, "correo", "email", "e-mail");

            for (int i = filaCabecera + 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                if (row == null) continue;
                String documento = leer(row, colNumDoc, fmt);
                if (documento.isBlank()) continue; // fila vacía
                filas++;

                try {
                    boolean[] usuarioNuevo = {false};
                    Usuario aprendiz = obtenerOCrearAprendiz(
                            documento,
                            leer(row, colTipoDoc, fmt),
                            leer(row, colNombre, fmt),
                            leer(row, colApellido, fmt),
                            leer(row, colTelefono, fmt),
                            leer(row, colCorreo, fmt),
                            usuarioNuevo);

                    matricular(aprendiz, ficha);

                    if (usuarioNuevo[0]) aprendicesCreados++;
                    matriculasCreadas++;
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo Excel: " + e.getMessage(), e);
        }

        return new ResultadoImportacionTecnico(filas, aprendicesCreados, matriculasCreadas, errores);
    }

    // ─────────────────────────────── Helpers de negocio ───────────────────────────────

    // Candado de autorización: la ficha debe estar asignada (y activa) al instructor logueado
    private Ficha fichaDelInstructor(Long idUsuarioInstructor, Long idFicha) {
        return listarFichas(idUsuarioInstructor).stream()
                .map(InstructorTecnicoFicha::getFicha)
                .filter(f -> f.getIdFicha().equals(idFicha))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La ficha seleccionada no está asignada a tu usuario."));
    }

    // La matrícula (APRENDIZ_FICHA) es única por usuario: si ya existe en otra ficha, se informa
    private void matricular(Usuario aprendiz, Ficha ficha) {
        AprendizFicha existente = aprendizFichaRepository.findByUsuarioIdUsuario(aprendiz.getIdUsuario()).orElse(null);
        if (existente != null) {
            if (existente.getFicha().getIdFicha().equals(ficha.getIdFicha())) {
                throw new IllegalArgumentException("El aprendiz con documento " + aprendiz.getDocumento()
                        + " ya está matriculado en la ficha " + ficha.getNumeroFicha() + ".");
            }
            throw new IllegalArgumentException("El aprendiz con documento " + aprendiz.getDocumento()
                    + " ya está matriculado en la ficha " + existente.getFicha().getNumeroFicha() + ".");
        }
        aprendizFichaRepository.save(AprendizFicha.builder()
                .usuario(aprendiz).ficha(ficha).estadoAcademico(EstadoAcademico.INICIADO).build());
    }

    private Usuario obtenerOCrearAprendiz(String documento, String tipoDoc, String nombre, String apellido,
                                          String telefono, String correo, boolean[] creado) {
        return usuarioRepository.findByDocumento(documento.trim()).orElseGet(() -> {
            creado[0] = true;
            Rol rolAprendiz = rolRepository.findByNombreRol("APRENDIZ")
                    .orElseThrow(() -> new IllegalStateException("No existe el rol APRENDIZ en el sistema."));

            // Si no llega correo se sintetiza uno (la columna CORREO es NOT NULL/UNIQUE en Oracle)
            String correoFinal = (correo == null || correo.isBlank())
                    ? documento.trim() + "@aprendiz.kronos.local"
                    : correo.trim();
            if (usuarioRepository.findByCorreoElectronico(correoFinal).isPresent()) {
                throw new IllegalArgumentException("El correo " + correoFinal + " ya está registrado para otro usuario.");
            }

            Usuario nuevo = Usuario.builder()
                    .tipoDocumento(parseTipoDocumento(tipoDoc))
                    .documento(recortar(documento.trim(), 10))
                    .nombre(recortar(vacioA(nombre, "Aprendiz"), 30))
                    .apellido(recortar(vacioA(apellido, "Nuevo"), 30))
                    .telefono(recortar(telefono, 11))
                    .correoElectronico(recortar(correoFinal, 150))
                    .password(passwordEncoder.encode(documento.trim()))
                    .debeCambiarContrasena(true) // obliga a cambiarla en su primer ingreso
                    .estado(true)
                    .usuarioRoles(new ArrayList<>())
                    .build();
            Usuario guardado = usuarioRepository.save(nuevo);
            guardado.getUsuarioRoles().add(UsuarioRol.builder().usuario(guardado).rol(rolAprendiz).build());
            return usuarioRepository.save(guardado);
        });
    }

    // ─────────────────────────────── Utilidades de parseo ───────────────────────────────

    private int ubicarCabecera(Sheet hoja, DataFormatter fmt) {
        int limite = Math.min(hoja.getLastRowNum(), 8);
        for (int i = hoja.getFirstRowNum(); i <= limite; i++) {
            Row row = hoja.getRow(i);
            if (row == null) continue;
            boolean tieneDoc = col(row, fmt, "documento") >= 0 || col(row, fmt, "cedula", "cédula") >= 0;
            boolean tieneNombre = col(row, fmt, "nombre") >= 0;
            if (tieneDoc && tieneNombre) return i;
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

    private int colTipoDocumento(Row cab, DataFormatter fmt) {
        for (Cell celda : cab) {
            String t = fmt.formatCellValue(celda).toLowerCase(Locale.ROOT).trim();
            if (t.contains("tipo") && t.contains("doc")) return celda.getColumnIndex();
        }
        return -1;
    }

    private int colNumeroDocumento(Row cab, DataFormatter fmt) {
        // Preferir "número documento"; si no, cualquier "documento"/"cédula" que no sea "tipo documento"
        int fallback = -1;
        for (Cell celda : cab) {
            String t = fmt.formatCellValue(celda).toLowerCase(Locale.ROOT).trim();
            boolean esDoc = t.contains("documento") || t.contains("cedula") || t.contains("cédula");
            if (!esDoc || t.contains("tipo")) continue;
            if (t.contains("num") || t.contains("n°") || t.contains("nro") || t.contains("#")) return celda.getColumnIndex();
            fallback = celda.getColumnIndex();
        }
        return fallback;
    }

    private String leer(Row row, int col, DataFormatter fmt) {
        if (col < 0) return "";
        Cell celda = row.getCell(col);
        return celda == null ? "" : fmt.formatCellValue(celda).trim();
    }

    private TipoDocumento parseTipoDocumento(String tipo) {
        if (tipo == null) return TipoDocumento.CC;
        String t = tipo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        if (t.startsWith("TI")) return TipoDocumento.TI;
        if (t.startsWith("CE")) return TipoDocumento.CE;
        if (t.startsWith("PA")) return TipoDocumento.PASAPORTE;
        return TipoDocumento.CC;
    }

    // ─────────────────────────────── Utilidades varias ───────────────────────────────

    private String etiquetaEstadoAcademico(EstadoAcademico estado) {
        if (estado == null) return SIN_DATO;
        return switch (estado) {
            case INICIADO -> "En formación";
            case POR_CERTIFICAR -> "Por certificar";
            case CERTIFICADO -> "Certificado";
        };
    }

    private String etiquetaModalidad(String modalidad) {
        return switch (modalidad) {
            case "PRESENCIAL" -> "Presencial";
            case "REMOTO" -> "Remoto";
            case "HIBRIDO" -> "Híbrido";
            default -> modalidad;
        };
    }

    private String fecha(LocalDate f) {
        return f != null ? f.format(FORMATO_FECHA) : SIN_DATO;
    }

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? SIN_DATO : texto;
    }

    private String vacioA(String texto, String porDefecto) {
        return (texto == null || texto.isBlank()) ? porDefecto : texto.trim();
    }

    private String recortar(String texto, int max) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
