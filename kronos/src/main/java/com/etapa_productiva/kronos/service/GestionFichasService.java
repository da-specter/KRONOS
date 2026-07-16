package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.AreasFormacion;
import com.etapa_productiva.kronos.entity.EstadoAcademico;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.NivelFormacion;
import com.etapa_productiva.kronos.entity.ProgramasFormacion;
import com.etapa_productiva.kronos.entity.Rol;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.UsuarioRol;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.AreasFormacionRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.FichaRepository;
import com.etapa_productiva.kronos.repository.NivelFormacionRepository;
import com.etapa_productiva.kronos.repository.ProgramasFormacionRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 📋 Módulo "Gestión de Fichas" del Gestor de Etapa: exporta el listado de fichas y el de
 * aprendices de una ficha (Excel/PDF) e importa un Excel de aprendices+ficha, creando/
 * actualizando las entidades Ficha, ProgramaFormacion, Usuario y AprendizFicha.
 */
@Service
public class GestionFichasService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SIN_DATO = "—";

    private static final String[] TITULOS_FICHAS = {
            "Ficha", "Programa de Formación", "Vigencia", "Estado",
            "Aprendices", "En Etapa Productiva", "Sin Etapa Productiva", "Por Certificar", "Certificados"
    };

    private static final String[] TITULOS_APRENDICES = {
            "Ficha", "Programa de Formación", "Vigencia", "Estado",
            "Nombre", "Apellido", "Tipo Documento", "Número Documento", "Teléfono",
            "Estado Académico", "En Etapa Productiva"
    };

    @Autowired
    private FichaRepository fichaRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private ProgramasFormacionRepository programasFormacionRepository;

    @Autowired
    private NivelFormacionRepository nivelFormacionRepository;

    @Autowired
    private AreasFormacionRepository areasFormacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─────────────────────────────── Exportación de fichas ───────────────────────────────

    private List<String[]> filasFichas() {
        List<String[]> filas = new ArrayList<>();
        for (Ficha ficha : fichaRepository.findAll()) {
            List<AprendizFicha> aprendices = aprendizFichaRepository.findByFichaIdFicha(ficha.getIdFicha());

            // Mismo criterio de clasificación que el dashboard del Instructor Técnico
            // (certificado > por certificar > tiene etapa > sin etapa), aplicado por ficha.
            Map<Long, EtapaProductiva> etapaPorAprendizFicha = new HashMap<>();
            for (EtapaProductiva etapa : etapaProductivaRepository.findByAprendizFichaFichaIdFicha(ficha.getIdFicha())) {
                etapaPorAprendizFicha.put(etapa.getAprendizFicha().getIdAprendizFicha(), etapa);
            }

            int enEtapa = 0, sinEtapa = 0, porCertificar = 0, certificados = 0;
            for (AprendizFicha matricula : aprendices) {
                String situacion = InstructorTecnicoService.clasificarSituacion(
                        matricula, etapaPorAprendizFicha.get(matricula.getIdAprendizFicha()));
                if (situacion.equals(InstructorTecnicoService.SIT_CERTIFICADO)) certificados++;
                else if (situacion.equals(InstructorTecnicoService.SIT_POR_CERTIFICAR)) porCertificar++;
                else if (situacion.equals(InstructorTecnicoService.SIT_EN_ETAPA)) enEtapa++;
                else sinEtapa++;
            }

            filas.add(new String[]{
                    ficha.getNumeroFicha(),
                    ficha.getProgramaFormacion().getNombrePrograma(),
                    vigencia(ficha),
                    Boolean.TRUE.equals(ficha.getEstado()) ? "Activa" : "Inactiva",
                    String.valueOf(aprendices.size()),
                    String.valueOf(enEtapa),
                    String.valueOf(sinEtapa),
                    String.valueOf(porCertificar),
                    String.valueOf(certificados)
            });
        }
        return filas;
    }

    public byte[] exportarFichasExcel() throws IOException {
        return ExportacionUtil.excel("Fichas", TITULOS_FICHAS, filasFichas());
    }

    public byte[] exportarFichasPdf() {
        return ExportacionUtil.pdf("KRONOS - Fichas Registradas", TITULOS_FICHAS, filasFichas());
    }

    // ─────────────────────── Exportación de aprendices de una ficha ───────────────────────

    private List<String[]> filasAprendicesDeFicha(Ficha ficha) {
        Set<Long> idsEnEtapa = idsAprendizFichaEnEtapa(ficha.getIdFicha());
        List<String[]> filas = new ArrayList<>();
        for (AprendizFicha matricula : aprendizFichaRepository.findByFichaIdFicha(ficha.getIdFicha())) {
            Usuario usuario = matricula.getUsuario();
            filas.add(new String[]{
                    ficha.getNumeroFicha(),
                    ficha.getProgramaFormacion().getNombrePrograma(),
                    vigencia(ficha),
                    Boolean.TRUE.equals(ficha.getEstado()) ? "Activa" : "Inactiva",
                    valor(usuario.getNombre()),
                    valor(usuario.getApellido()),
                    usuario.getTipoDocumento() != null ? usuario.getTipoDocumento().name() : SIN_DATO,
                    valor(usuario.getDocumento()),
                    valor(usuario.getTelefono()),
                    matricula.getEstadoAcademico() != null ? matricula.getEstadoAcademico().name() : SIN_DATO,
                    idsEnEtapa.contains(matricula.getIdAprendizFicha()) ? "Sí" : "No"
            });
        }
        return filas;
    }

    public Ficha buscarFicha(Long idFicha) {
        return fichaRepository.findById(idFicha)
                .orElseThrow(() -> new IllegalArgumentException("La ficha indicada no existe."));
    }

    public byte[] exportarAprendicesFichaExcel(Ficha ficha) throws IOException {
        return ExportacionUtil.excel("Aprendices ficha " + ficha.getNumeroFicha(),
                TITULOS_APRENDICES, filasAprendicesDeFicha(ficha));
    }

    public byte[] exportarAprendicesFichaPdf(Ficha ficha) {
        return ExportacionUtil.pdf("KRONOS - Aprendices de la ficha " + ficha.getNumeroFicha(),
                TITULOS_APRENDICES, filasAprendicesDeFicha(ficha));
    }

    // ───────────────────────────────── Importación ─────────────────────────────────

    /** Resultado resumido de una importación, para informarlo al Gestor. */
    public record ResultadoImportacion(int filas, int fichasCreadas, int programasCreados,
                                       int aprendicesCreados, int matriculasCreadas, List<String> errores) {
    }

    /**
     * 📋 Plantilla Excel en blanco para la importación de aprendices+ficha: hoja de datos
     * con los encabezados que espera {@link #importar} y hoja "Guía" con las reglas de
     * diligenciamiento y de credenciales (correo = usuario, documento = contraseña inicial).
     */
    public byte[] generarPlantillaImportacion() throws IOException {
        String[] titulos = {"Ficha", "Programa", "Vigencia", "Estado",
                "Nombre", "Apellido", "Tipo Documento", "Número Documento", "Correo Electrónico", "Teléfono"};

        List<String[]> guia = List.of(
                new String[]{"Ficha", "Sí", "Número de la ficha (máx. 7 caracteres). Ej: 3063365"},
                new String[]{"Programa", "Recomendada", "Nombre del programa de formación. Ej: Análisis y Desarrollo de Software. Si se omite, la ficha queda como \"SIN PROGRAMA\"."},
                new String[]{"Vigencia", "No", "Fecha de inicio y fin de la ficha separadas por \"→\" o la letra \"a\". Ej: 01/02/2026 → 01/02/2028. Si se omite: hoy más 1 año."},
                new String[]{"Estado", "No", "Estado de la ficha: Activa o Inactiva. Si se omite: Activa."},
                new String[]{"Nombre", "Sí", "Nombres del aprendiz. Ej: Laura Valentina"},
                new String[]{"Apellido", "Sí", "Apellidos del aprendiz. Ej: García Pérez"},
                new String[]{"Tipo Documento", "No", "CC, TI, CE o PASAPORTE. Si se omite: CC."},
                new String[]{"Número Documento", "Sí", "Solo dígitos, sin puntos ni espacios (máx. 10). Ej: 1023456789"},
                new String[]{"Correo Electrónico", "Recomendada", "Correo personal del aprendiz: será su USUARIO de ingreso a KRONOS. Ej: laura.garcia@correo.com"},
                new String[]{"Teléfono", "No", "Celular de contacto, solo dígitos (máx. 11). Ej: 3001234567"});

        List<String> notas = List.of(
                "NOTAS DE USO:",
                "• Diligencia los datos a partir de la fila 2 de la hoja de datos; no modifiques ni elimines la fila de encabezados.",
                "• La contraseña inicial de cada aprendiz nuevo es su número de documento y el sistema le exigirá cambiarla en su primer ingreso.",
                "• Si no indicas correo, KRONOS genera uno interno (documento@aprendiz.kronos.local); el correo no puede repetirse entre usuarios.",
                "• Si la ficha o el programa no existen, se crean automáticamente; si el aprendiz ya existe (mismo documento), solo se matricula en la ficha.");

        return ExportacionUtil.plantillaExcel("Aprendices", titulos, guia, notas);
    }

    @Transactional
    public ResultadoImportacion importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar el archivo Excel a importar.");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!nombre.endsWith(".xlsx") && !nombre.endsWith(".xls")) {
            throw new IllegalArgumentException("El archivo a importar debe ser un Excel (.xlsx o .xls).");
        }

        int filas = 0, fichasCreadas = 0, programasCreados = 0, aprendicesCreados = 0, matriculasCreadas = 0;
        List<String> errores = new ArrayList<>();

        try (InputStream in = archivo.getInputStream(); Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            int filaCabecera = ubicarCabecera(hoja, fmt);
            if (filaCabecera < 0) {
                throw new IllegalArgumentException("No se encontró la fila de encabezados (se esperan columnas como 'Nombre', 'Documento', 'Ficha').");
            }

            Row cab = hoja.getRow(filaCabecera);
            int colFicha = col(cab, fmt, "ficha");
            int colPrograma = col(cab, fmt, "programa");
            int colVigencia = col(cab, fmt, "vigencia");
            int colEstado = colEstadoFicha(cab, fmt);
            int colNombre = col(cab, fmt, "nombre");
            int colApellido = col(cab, fmt, "apellido");
            int colTipoDoc = colTipoDocumento(cab, fmt);
            int colNumDoc = colNumeroDocumento(cab, fmt);
            int colTelefono = col(cab, fmt, "tel", "teléfono", "telefono", "celular");
            int colCorreo = col(cab, fmt, "correo", "email", "e-mail");

            if (colNumDoc < 0 || colNombre < 0 || colFicha < 0) {
                throw new IllegalArgumentException("Faltan columnas obligatorias: se requieren al menos 'Ficha', 'Nombre' y 'Número Documento'.");
            }

            for (int i = filaCabecera + 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                if (row == null) continue;
                String documento = leer(row, colNumDoc, fmt);
                String numeroFicha = leer(row, colFicha, fmt);
                if (documento.isBlank() && numeroFicha.isBlank()) continue; // fila vacía
                filas++;

                try {
                    // 1) Programa (reutiliza o crea con área/nivel base)
                    boolean[] programaNuevoFlag = {false};
                    ProgramasFormacion programa = obtenerOCrearPrograma(leer(row, colPrograma, fmt), programaNuevoFlag);
                    boolean programaNuevo = programaNuevoFlag[0];

                    // 2) Ficha (reutiliza por número o crea)
                    LocalDate[] rango = parseVigencia(leer(row, colVigencia, fmt));
                    boolean estadoFicha = parseEstado(leer(row, colEstado, fmt));
                    boolean[] fichaNueva = {false};
                    Ficha ficha = obtenerOCrearFicha(numeroFicha, programa, rango, estadoFicha, fichaNueva);

                    // 3) Usuario aprendiz (reutiliza por documento o crea)
                    boolean[] usuarioNuevo = {false};
                    Usuario usuario = obtenerOCrearAprendiz(
                            documento,
                            leer(row, colTipoDoc, fmt),
                            leer(row, colNombre, fmt),
                            leer(row, colApellido, fmt),
                            leer(row, colTelefono, fmt),
                            leer(row, colCorreo, fmt),
                            usuarioNuevo);

                    // 4) Matrícula (AprendizFicha) si no existe
                    boolean matriculaNueva = false;
                    if (!aprendizFichaRepository.existsByUsuarioIdUsuarioAndFichaIdFicha(usuario.getIdUsuario(), ficha.getIdFicha())) {
                        aprendizFichaRepository.save(AprendizFicha.builder()
                                .usuario(usuario).ficha(ficha).estadoAcademico(EstadoAcademico.INICIADO).build());
                        matriculaNueva = true;
                    }

                    if (programaNuevo) programasCreados++;
                    if (fichaNueva[0]) fichasCreadas++;
                    if (usuarioNuevo[0]) aprendicesCreados++;
                    if (matriculaNueva) matriculasCreadas++;
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo Excel: " + e.getMessage(), e);
        }

        return new ResultadoImportacion(filas, fichasCreadas, programasCreados, aprendicesCreados, matriculasCreadas, errores);
    }

    // ─────────────────────────────── Helpers de importación ───────────────────────────────

    private ProgramasFormacion obtenerOCrearPrograma(String nombre, boolean[] creado) {
        String limpio = (nombre == null || nombre.isBlank()) ? "SIN PROGRAMA" : nombre.trim();
        return programasFormacionRepository.findFirstByNombreProgramaIgnoreCase(limpio)
                .orElseGet(() -> {
                    NivelFormacion nivel = nivelFormacionRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("No hay niveles de formación base para crear el programa."));
                    AreasFormacion area = areasFormacionRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("No hay áreas de formación base para crear el programa."));
                    creado[0] = true;
                    return programasFormacionRepository.save(ProgramasFormacion.builder()
                            .nombrePrograma(limpio).estado(true).areaFormacion(area).nivelFormacion(nivel).build());
                });
    }

    private Ficha obtenerOCrearFicha(String numeroFicha, ProgramasFormacion programa,
                                     LocalDate[] rango, boolean estado, boolean[] creada) {
        String numero = numeroFicha.isBlank() ? "SIN-FICHA" : numeroFicha.trim();
        return fichaRepository.findByNumeroFicha(numero).orElseGet(() -> {
            creada[0] = true;
            return fichaRepository.save(Ficha.builder()
                    .numeroFicha(numero.length() > 7 ? numero.substring(0, 7) : numero)
                    .fechaInicio(rango[0]).fechaFin(rango[1]).estado(estado)
                    .programaFormacion(programa).build());
        });
    }

    private Usuario obtenerOCrearAprendiz(String documento, String tipoDoc, String nombre,
                                          String apellido, String telefono, String correo, boolean[] creado) {
        return usuarioRepository.findByDocumento(documento.trim()).orElseGet(() -> {
            creado[0] = true;
            Rol rolAprendiz = rolRepository.findByNombreRol("APRENDIZ")
                    .orElseThrow(() -> new IllegalStateException("No existe el rol APRENDIZ en el sistema."));

            // El correo del Excel será el usuario de ingreso; si no llega, se sintetiza uno
            // interno para cumplir las restricciones NOT NULL/UNIQUE de la columna CORREO
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
                    .apellido(recortar(vacioA(apellido, "Importado"), 30))
                    .telefono(recortar(telefono, 11))
                    .correoElectronico(recortar(correoFinal, 150))
                    .password(passwordEncoder.encode(documento.trim()))
                    .debeCambiarContrasena(true) // contraseña inicial = documento: forzar cambio en el primer login
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

    // "Estado" de la ficha, evitando confundir con "Estado Académico"/"En Etapa"
    private int colEstadoFicha(Row cab, DataFormatter fmt) {
        for (Cell celda : cab) {
            String t = fmt.formatCellValue(celda).toLowerCase(Locale.ROOT).trim();
            if (t.equals("estado") || (t.contains("estado") && !t.contains("acad") && !t.contains("etapa"))) {
                return celda.getColumnIndex();
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
            if (!esDoc || (t.contains("tipo"))) continue;
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

    private LocalDate[] parseVigencia(String vigencia) {
        LocalDate inicio = LocalDate.now();
        LocalDate fin = inicio.plusYears(1);
        if (vigencia != null && !vigencia.isBlank()) {
            String[] partes = vigencia.split("→|->|—|~|\\ba\\b");
            if (partes.length >= 1) { LocalDate d = parseFecha(partes[0]); if (d != null) inicio = d; }
            if (partes.length >= 2) { LocalDate d = parseFecha(partes[1]); if (d != null) fin = d; }
        }
        if (fin.isBefore(inicio)) fin = inicio.plusYears(1);
        return new LocalDate[]{inicio, fin};
    }

    private LocalDate parseFecha(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        if (t.isEmpty()) return null;
        String[] patrones = {"dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "d/M/yy", "dd/MM/yy"};
        for (String p : patrones) {
            try {
                return LocalDate.parse(t, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {
                // probar el siguiente patrón
            }
        }
        return null;
    }

    private boolean parseEstado(String estado) {
        if (estado == null) return true;
        String e = estado.trim().toLowerCase(Locale.ROOT);
        if (e.isEmpty()) return true;
        return e.startsWith("activ") || e.equals("1") || e.equals("true") || e.equals("sí") || e.equals("si");
    }

    private TipoDocumento parseTipoDocumento(String tipo) {
        if (tipo == null) return TipoDocumento.CC;
        String t = tipo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        if (t.startsWith("TI")) return TipoDocumento.TI;
        if (t.startsWith("CE")) return TipoDocumento.CE;
        if (t.startsWith("PA") || t.startsWith("PAS")) return TipoDocumento.PASAPORTE;
        return TipoDocumento.CC;
    }

    // ─────────────────────────────── Utilidades varias ───────────────────────────────

    private Set<Long> idsAprendizFichaEnEtapa(Long idFicha) {
        Set<Long> ids = new HashSet<>();
        for (EtapaProductiva etapa : etapaProductivaRepository.findByAprendizFichaFichaIdFicha(idFicha)) {
            ids.add(etapa.getAprendizFicha().getIdAprendizFicha());
        }
        return ids;
    }

    private String vigencia(Ficha ficha) {
        return fecha(ficha.getFechaInicio()) + " → " + fecha(ficha.getFechaFin());
    }

    private String fecha(LocalDate fecha) {
        return fecha != null ? fecha.format(FORMATO_FECHA) : SIN_DATO;
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
