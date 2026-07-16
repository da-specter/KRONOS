package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.AprendizBitacoraDetalleDto;
import com.etapa_productiva.kronos.dto.AprendizBitacoraResumenDto;
import com.etapa_productiva.kronos.dto.AprendizPlaneacionResumenDto;
import com.etapa_productiva.kronos.dto.BitacoraEvaluarDto;
import com.etapa_productiva.kronos.dto.CertificacionInfoDto;
import com.etapa_productiva.kronos.dto.DashboardBitacorasDto;
import com.etapa_productiva.kronos.dto.FactorEstadoDto;
import com.etapa_productiva.kronos.dto.DashboardInstructorDto.BarraGrafico;
import com.etapa_productiva.kronos.dto.DashboardInstructorDto.SegmentoGrafico;
import com.etapa_productiva.kronos.dto.MomentoEstadoDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.Bitacora;
import com.etapa_productiva.kronos.entity.CategoriaFactor;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.DocumentoRequisito;
import com.etapa_productiva.kronos.entity.EstadoAcademico;
import com.etapa_productiva.kronos.entity.EstadoBitacora;
import com.etapa_productiva.kronos.entity.EstadoEtapa;
import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.EvaluacionBitacora;
import com.etapa_productiva.kronos.entity.EvaluacionMomento;
import com.etapa_productiva.kronos.entity.FactorMomento;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.FormatoPlaneacion;
import com.etapa_productiva.kronos.entity.InstructorSeguimiento;
import com.etapa_productiva.kronos.entity.JuicioEvaluacion;
import com.etapa_productiva.kronos.entity.ModalidadEtapa;
import com.etapa_productiva.kronos.entity.ModalidadFirma;
import com.etapa_productiva.kronos.entity.ResultadoEvaluacion;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.ValoracionFactor;
import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.BitacoraRepository;
import com.etapa_productiva.kronos.repository.CronogramaBitacorasRepository;
import com.etapa_productiva.kronos.repository.DocumentoRequisitoRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.EvaluacionBitacoraRepository;
import com.etapa_productiva.kronos.repository.EvaluacionMomentoRepository;
import com.etapa_productiva.kronos.repository.FactorMomentoRepository;
import com.etapa_productiva.kronos.repository.FormatoPlaneacionRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.repository.VisitaSeguimientoRepository;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🛠️ Módulo "Evaluación de Formatos" del Instructor de Seguimiento: revisa y califica las
 * Bitácoras y el Formato de Planeación (023) de los aprendices que tiene asignados (vía
 * ASIGNACION_INSTRUCTOR_ETAPA vigente) — descarga el archivo, evalúa (Aprobado/Reprobado/
 * Corregir), deja la novedad si aplica y notifica al aprendiz. También arma, junto con el
 * Aprendiz, el Formato 023 (GFPI-F-023) por "momentos": aprendiz e instructor diligencian su
 * parte en paralelo y KRONOS genera el PDF automáticamente al completarse los 3.
 */
@Service
public class EvaluacionFormatosService {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SELLO_TIEMPO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String SIN_DATO = "—";

    private static final int BITACORAS_POR_MOMENTO = 4;
    private static final int TOTAL_MOMENTOS = 3;
    private static final String[] ETIQUETAS_MOMENTO = {
            "MOMENTO N°1 - PLANEACIÓN DE LA ETAPA PRODUCTIVA",
            "MOMENTO N°2 - SEGUIMIENTO ETAPA PRODUCTIVA",
            "MOMENTO N°3 - EVALUACIÓN ETAPA PRODUCTIVA"
    };

    // Valores predeterminados fijos del encabezado (decisión explícita del usuario)
    private static final String REGIONAL_PREDETERMINADA = "Antioquia";
    private static final String CENTRO_PREDETERMINADO = "CTAPT";
    private static final String ESTRATEGIA_PREDETERMINADA = "TITULADA";

    // Las 13 variables de los Factores Técnicos/Actitudinales, tal como aparecen en el GFPI-F-023 real
    private static final String[] FACTORES_TECNICOS = {
            "Aplicación de conocimiento", "Mejora continua", "Fortalecimiento ocupacional",
            "Oportunidad y calidad", "Responsabilidad ambiental", "Administración de recursos",
            "Seguridad y salud en el trabajo", "Documentación etapa productiva"
    };
    private static final String[] FACTORES_ACTITUDINALES = {
            "Relaciones interpersonales", "Trabajo en equipo", "Solución de problemas",
            "Cumplimiento", "Organización"
    };

    private static final LinkedHashMap<String, String> COLOR_RESULTADO = new LinkedHashMap<>();
    static {
        COLOR_RESULTADO.put("En revisión", "#D97706");
        COLOR_RESULTADO.put("Aprobada", "#057015");
        COLOR_RESULTADO.put("Para corregir", "#2563EB");
        COLOR_RESULTADO.put("Reprobada", "#DC2626");
    }

    @Autowired
    private InstructorSeguimientoRepository instructorSeguimientoRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private BitacoraRepository bitacoraRepository;

    @Autowired
    private EvaluacionBitacoraRepository evaluacionBitacoraRepository;

    @Autowired
    private FormatoPlaneacionRepository formatoPlaneacionRepository;

    @Autowired
    private EvaluacionMomentoRepository evaluacionMomentoRepository;

    @Autowired
    private FactorMomentoRepository factorMomentoRepository;

    @Autowired
    private DocumentoRequisitoRepository documentoRequisitoRepository;

    @Autowired
    private GeneradorFormato023HtmlService generadorFormato023HtmlService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private CronogramaBitacorasRepository cronogramaBitacorasRepository;

    @Autowired
    private VisitaSeguimientoRepository visitaSeguimientoRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    // Etapas Productivas con asignación vigente del Instructor de Seguimiento (sus aprendices)
    private List<EtapaProductiva> etapasAsignadas(Long idUsuarioInstructor) {
        return instructorSeguimientoRepository.findByUsuarioIdUsuario(idUsuarioInstructor)
                .map(InstructorSeguimiento::getIdInstructorSeguimiento)
                .map(asignacionInstructorEtapaRepository::findByInstructorIdInstructorSeguimientoAndEstadoAsignacionTrue)
                .orElse(new ArrayList<>())
                .stream()
                .map(AsignacionInstructorEtapa::getEtapaProductiva)
                .toList();
    }

    // ─────────────────────────────────── Bitácoras ───────────────────────────────────

    /** Un renglón por aprendiz asignado al instructor, con el resumen de sus bitácoras. */
    @Transactional(readOnly = true)
    public List<AprendizBitacoraResumenDto> listarResumenBitacoras(Long idUsuarioInstructor) {
        List<AprendizBitacoraResumenDto> filas = new ArrayList<>();

        for (EtapaProductiva etapa : etapasAsignadas(idUsuarioInstructor)) {
            Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
            List<Bitacora> bitacoras = bitacoraRepository.findByEtapaProductivaIdEtapaOrderByFechaEntregaDesc(etapa.getIdEtapa());

            int aprobadas = 0, enRevision = 0, corregir = 0, reprobadas = 0;
            for (Bitacora b : bitacoras) {
                EvaluacionBitacora ev = evaluacionBitacoraRepository
                        .findTopByBitacoraIdBitacoraOrderByFechaEvaluacionDesc(b.getIdBitacora()).orElse(null);
                if (ev == null) {
                    enRevision++;
                    continue;
                }
                switch (ev.getResultado()) {
                    case APROBADO -> aprobadas++;
                    case CORREGIR -> corregir++;
                    case REPROBADO -> reprobadas++;
                }
            }

            filas.add(AprendizBitacoraResumenDto.builder()
                    .idEtapa(etapa.getIdEtapa())
                    .nombres(valor(aprendiz.getNombre()))
                    .apellidos(valor(aprendiz.getApellido()))
                    .documento(valor(aprendiz.getDocumento()))
                    .ficha(etapa.getAprendizFicha().getFicha().getNumeroFicha())
                    .totalSubidas(bitacoras.size())
                    .aprobadas(aprobadas)
                    .enRevision(enRevision)
                    .corregir(corregir)
                    .reprobadas(reprobadas)
                    .build());
        }

        filas.sort(Comparator.comparing(AprendizBitacoraResumenDto::getApellidos, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AprendizBitacoraResumenDto::getNombres, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    /** Calcula números y series de gráficas a partir del resumen de bitácoras. */
    public DashboardBitacorasDto calcularDashboardBitacoras(List<AprendizBitacoraResumenDto> filas) {
        int totalSubidas = 0, aprobadas = 0, enRevision = 0, corregir = 0, reprobadas = 0;
        Map<String, Integer> porResultado = new LinkedHashMap<>();
        COLOR_RESULTADO.keySet().forEach(k -> porResultado.put(k, 0));

        List<BarraGrafico> barrasBase = new ArrayList<>();
        for (AprendizBitacoraResumenDto f : filas) {
            totalSubidas += f.getTotalSubidas();
            aprobadas += f.getAprobadas();
            enRevision += f.getEnRevision();
            corregir += f.getCorregir();
            reprobadas += f.getReprobadas();
            porResultado.merge("Aprobada", f.getAprobadas(), Integer::sum);
            porResultado.merge("En revisión", f.getEnRevision(), Integer::sum);
            porResultado.merge("Para corregir", f.getCorregir(), Integer::sum);
            porResultado.merge("Reprobada", f.getReprobadas(), Integer::sum);
            if (f.getTotalSubidas() > 0) {
                barrasBase.add(BarraGrafico.builder()
                        .etiqueta(f.getNombres() + " " + f.getApellidos())
                        .cantidad(f.getTotalSubidas())
                        .build());
            }
        }

        int total = totalSubidas;
        List<SegmentoGrafico> segmentos = new ArrayList<>();
        StringBuilder gradiente = new StringBuilder("conic-gradient(");
        double gradoActual = 0;
        boolean primero = true;
        for (Map.Entry<String, String> e : COLOR_RESULTADO.entrySet()) {
            int cant = porResultado.getOrDefault(e.getKey(), 0);
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

        int maxBarra = barrasBase.stream().mapToInt(BarraGrafico::getCantidad).max().orElse(1);
        List<BarraGrafico> barras = new ArrayList<>();
        barrasBase.forEach(b -> barras.add(BarraGrafico.builder()
                .etiqueta(b.getEtiqueta()).cantidad(b.getCantidad())
                .porcentaje((int) Math.round(b.getCantidad() * 100.0 / maxBarra)).build()));
        barras.sort(Comparator.comparingInt(BarraGrafico::getCantidad).reversed());

        return DashboardBitacorasDto.builder()
                .totalSubidas(totalSubidas)
                .aprobadas(aprobadas)
                .enRevision(enRevision)
                .corregir(corregir)
                .reprobadas(reprobadas)
                .donutGradient(gradiente.toString())
                .segmentosResultado(segmentos)
                .bitacorasPorAprendiz(barras)
                .build();
    }

    /** Detalle de un aprendiz (etapa) para descargar y evaluar cada una de sus bitácoras. */
    @Transactional(readOnly = true)
    public AprendizBitacoraDetalleDto listarBitacorasDeEtapa(Long idUsuarioInstructor, Long idEtapa) {
        EtapaProductiva etapa = etapaDelInstructor(idUsuarioInstructor, idEtapa);
        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();

        List<BitacoraEvaluarDto> bitacoras = new ArrayList<>();
        for (Bitacora b : bitacoraRepository.findByEtapaProductivaIdEtapaOrderByFechaEntregaDesc(idEtapa)) {
            EvaluacionBitacora ev = evaluacionBitacoraRepository
                    .findTopByBitacoraIdBitacoraOrderByFechaEvaluacionDesc(b.getIdBitacora()).orElse(null);
            bitacoras.add(BitacoraEvaluarDto.builder()
                    .idBitacora(b.getIdBitacora())
                    .numeroBitacora(b.getCronogramaBitacora().getNumeroBitacora())
                    .asunto(b.getAsunto())
                    .rutaArchivo(b.getRutaArchivo())
                    .fechaSubida(b.getFechaHoraSubida().format(FORMATO_FECHA_HORA))
                    .resultado(ev != null ? ev.getResultado().name() : null)
                    .observaciones(ev != null ? ev.getObservaciones() : null)
                    .build());
        }

        return AprendizBitacoraDetalleDto.builder()
                .idEtapa(idEtapa)
                .nombres(valor(aprendiz.getNombre()))
                .apellidos(valor(aprendiz.getApellido()))
                .documento(valor(aprendiz.getDocumento()))
                .ficha(etapa.getAprendizFicha().getFicha().getNumeroFicha())
                .bitacoras(bitacoras)
                .build();
    }

    /** ✅ Registra la evaluación de una bitácora y notifica el resultado al aprendiz. */
    @Transactional
    public void evaluarBitacora(Long idUsuarioInstructor, Long idBitacora, ResultadoEvaluacion resultado, String observaciones) {
        Bitacora bitacora = bitacoraRepository.findById(idBitacora)
                .orElseThrow(() -> new IllegalArgumentException("La bitácora indicada no existe."));
        EtapaProductiva etapa = bitacora.getCronogramaBitacora().getEtapaProductiva();
        validarEtapaAsignada(idUsuarioInstructor, etapa);

        String observacionLimpia = limpiarObservacion(resultado, observaciones);

        Usuario instructor = usuarioRepository.findById(idUsuarioInstructor)
                .orElseThrow(() -> new IllegalStateException("No se encontró tu usuario."));

        evaluacionBitacoraRepository.save(EvaluacionBitacora.builder()
                .bitacora(bitacora)
                .instructor(instructor)
                .resultado(resultado)
                .observaciones(observacionLimpia)
                .build());

        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
        int numero = bitacora.getCronogramaBitacora().getNumeroBitacora();
        notificacionService.crear(aprendiz, mensajeEvaluacion("Bitácora N°" + numero, resultado, observacionLimpia, true), "/aprendiz/bitacoras");

        verificarYTransicionarPorCertificar(etapa);
    }

    // ─────────────────────────── Formato de Planeación (023) ───────────────────────────

    /** Un renglón por aprendiz asignado al instructor, con el estado de sus 3 "momentos". */
    @Transactional
    public List<AprendizPlaneacionResumenDto> listarResumenPlaneacion(Long idUsuarioInstructor) {
        List<AprendizPlaneacionResumenDto> filas = new ArrayList<>();

        for (EtapaProductiva etapa : etapasAsignadas(idUsuarioInstructor)) {
            Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
            asegurarMomentosSembrados(etapa);
            List<MomentoEstadoDto> momentos = construirEstadoMomentos(etapa);
            int completados = (int) momentos.stream()
                    .filter(m -> m.isAprendizCompleto() && m.isInstructorCompleto()).count();

            FormatoPlaneacion formato = formatoPlaneacionRepository.findByEtapaProductivaIdEtapa(etapa.getIdEtapa()).orElse(null);

            filas.add(AprendizPlaneacionResumenDto.builder()
                    .idEtapa(etapa.getIdEtapa())
                    .nombres(valor(aprendiz.getNombre()))
                    .apellidos(valor(aprendiz.getApellido()))
                    .documento(valor(aprendiz.getDocumento()))
                    .ficha(etapa.getAprendizFicha().getFicha().getNumeroFicha())
                    .momentos(momentos)
                    .momentosCompletados(completados)
                    .formatoGenerado(formato != null)
                    .rutaArchivo023(formato != null ? formato.getRutaArchivo() : null)
                    .fechaGeneracion023(formato != null ? formato.getFechaHoraSubida().format(FORMATO_FECHA_HORA) : null)
                    .firmaInstructorSubida(etapa.getFirmaInstructorRuta() != null)
                    .build());
        }

        filas.sort(Comparator.comparing(AprendizPlaneacionResumenDto::getApellidos, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AprendizPlaneacionResumenDto::getNombres, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    // ─────────────────────── Exportación del listado Formato 023 ───────────────────────

    private static final String[] TITULOS_PLANEACION = {
            "Nombres", "Apellidos", "Documento", "Ficha",
            "Momento 1", "Momento 2", "Momento 3",
            "Momentos Completados", "Estado 023", "Fecha Generación 023"
    };

    public byte[] exportarPlaneacionExcel(List<AprendizPlaneacionResumenDto> resumen) throws IOException {
        return ExportacionUtil.excel("Formato 023", TITULOS_PLANEACION, filasPlaneacion(resumen));
    }

    public byte[] exportarPlaneacionPdf(List<AprendizPlaneacionResumenDto> resumen) {
        return ExportacionUtil.pdf("KRONOS - Formato de Planeación 023", TITULOS_PLANEACION, filasPlaneacion(resumen));
    }

    private List<String[]> filasPlaneacion(List<AprendizPlaneacionResumenDto> resumen) {
        List<String[]> filas = new ArrayList<>();
        for (AprendizPlaneacionResumenDto p : resumen) {
            List<MomentoEstadoDto> momentos = p.getMomentos();
            filas.add(new String[]{
                    valor(p.getNombres()),
                    valor(p.getApellidos()),
                    valor(p.getDocumento()),
                    valor(p.getFicha()),
                    estadoMomento(momentos, 1),
                    estadoMomento(momentos, 2),
                    estadoMomento(momentos, 3),
                    p.getMomentosCompletados() + " / 3",
                    p.isFormatoGenerado() ? "Generado" : "Pendiente",
                    p.getFechaGeneracion023() != null ? p.getFechaGeneracion023() : SIN_DATO
            });
        }
        return filas;
    }

    // Texto legible del estado de un momento (1, 2 o 3) para la exportación
    private String estadoMomento(List<MomentoEstadoDto> momentos, int numero) {
        if (momentos == null) return SIN_DATO;
        MomentoEstadoDto m = momentos.stream().filter(x -> x.getNumero() == numero).findFirst().orElse(null);
        if (m == null) return SIN_DATO;
        if (!m.isHabilitado()) return "Bloqueado";
        if (m.isAprendizCompleto() && m.isInstructorCompleto()) return "Completo";
        if (m.isAprendizCompleto()) return "Falta instructor";
        if (m.isInstructorCompleto()) return "Falta aprendiz";
        return "En proceso";
    }

    /** Estado de los 3 momentos de una etapa (público: lo usa también el dashboard del Aprendiz). */
    @Transactional
    public List<MomentoEstadoDto> obtenerMomentos(EtapaProductiva etapa) {
        asegurarMomentosSembrados(etapa);
        return construirEstadoMomentos(etapa);
    }

    /** El Formato 023 generado por KRONOS para una etapa, si ya existe. */
    @Transactional(readOnly = true)
    public FormatoPlaneacion obtenerFormato023(Long idEtapa) {
        return formatoPlaneacionRepository.findByEtapaProductivaIdEtapa(idEtapa).orElse(null);
    }

    /**
     * ✍️ Registra la parte del INSTRUCTOR en un momento (observación y, en el Momento 3, el
     * juicio de evaluación final) y notifica al aprendiz. Si con este guardado el momento y los
     * anteriores quedan completos por ambos lados, regenera el PDF y evalúa la certificación.
     */
    @Transactional
    public void guardarObservacionMomento(Long idUsuarioInstructor, Long idEtapa, int numeroMomento,
            String observacion, JuicioEvaluacion juicioEvaluacion,
            String retroInstructorProceso, String retroInstructorDesempeno, String enlaceGrabacion) {
        if (numeroMomento < 1 || numeroMomento > TOTAL_MOMENTOS) {
            throw new IllegalArgumentException("El momento indicado no es válido.");
        }
        boolean esMomento3 = numeroMomento == TOTAL_MOMENTOS;
        if (!esMomento3 && (observacion == null || observacion.isBlank())) {
            throw new IllegalArgumentException("Debes escribir la observación del momento.");
        }
        if (esMomento3 && (retroInstructorProceso == null || retroInstructorProceso.isBlank()
                || retroInstructorDesempeno == null || retroInstructorDesempeno.isBlank())) {
            throw new IllegalArgumentException("Debes escribir tu retroalimentación (proceso de formación y desempeño de competencias).");
        }
        if (esMomento3 && juicioEvaluacion == null) {
            throw new IllegalArgumentException("Debes seleccionar el juicio de evaluación (Aprobado / No aprobado).");
        }

        EtapaProductiva etapa = etapaDelInstructor(idUsuarioInstructor, idEtapa);

        List<MomentoEstadoDto> momentos = construirEstadoMomentos(etapa);
        if (!momentos.get(numeroMomento - 1).isHabilitado()) {
            throw new IllegalArgumentException("Este momento todavía no se ha habilitado.");
        }

        Usuario instructor = usuarioRepository.findById(idUsuarioInstructor)
                .orElseThrow(() -> new IllegalStateException("No se encontró tu usuario."));

        EvaluacionMomento registro = obtenerOCrearMomento(etapa, numeroMomento);
        registro.setInstructor(instructor);
        if (esMomento3) {
            registro.setRetroInstructorProceso(retroInstructorProceso.trim());
            registro.setRetroInstructorDesempeno(retroInstructorDesempeno.trim());
            registro.setJuicioEvaluacion(juicioEvaluacion);
        } else {
            registro.setObservacion(observacion.trim());
        }
        registro.setEnlaceGrabacion(vacioANull(enlaceGrabacion));
        registro = evaluacionMomentoRepository.save(registro);

        if (numeroMomento >= 2) {
            sembrarFactoresSiNoExisten(registro);
        }

        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
        notificacionService.crear(aprendiz, "📋 Tu Instructor de Seguimiento registró la observación del Momento "
                + numeroMomento + " de tu Formato 023.", "/aprendiz/bitacoras");

        regenerarSiCompleto(etapa);
    }

    /**
     * ✍️ Registra la parte del APRENDIZ en un momento (datos de planeación en el Momento 1,
     * observación propia en cualquier momento, y metadatos comunes de firma). Notifica al
     * instructor asignado y, si con esto el momento queda completo por ambos lados, dispara la
     * misma regeneración/certificación que el lado del instructor.
     */
    @Transactional
    public void guardarDatosAprendizMomento(Long idUsuarioAprendiz, Long idEtapa, int numeroMomento,
            String competenciasDesarrollar, String resultadosAprendizaje, String actividadesDesarrollar,
            String evidenciaDescripcion, MultipartFile evidenciaArchivo, String observacionAprendiz,
            String ciudad, LocalDate fechaDiligenciamiento, ModalidadFirma modalidadFirma,
            LocalDate fechaMomento, String observacionEnteCoformador,
            String retroAprendizProceso, String retroAprendizDesempeno,
            String retroEnteProceso, String retroEnteDesempeno,
            List<Long> idFactor, List<String> valoracionFactor, List<String> observacionFactor) {

        if (numeroMomento < 1 || numeroMomento > TOTAL_MOMENTOS) {
            throw new IllegalArgumentException("El momento indicado no es válido.");
        }

        EtapaProductiva etapa = etapaDelAprendiz(idUsuarioAprendiz, idEtapa);

        List<MomentoEstadoDto> momentos = construirEstadoMomentos(etapa);
        if (!momentos.get(numeroMomento - 1).isHabilitado()) {
            throw new IllegalArgumentException("Este momento todavía no se ha habilitado.");
        }

        EvaluacionMomento registro = obtenerOCrearMomento(etapa, numeroMomento);

        if (numeroMomento == 1) {
            registro.setCompetenciasDesarrollar(vacioANull(competenciasDesarrollar));
            registro.setResultadosAprendizaje(vacioANull(resultadosAprendizaje));
            registro.setActividadesDesarrollar(vacioANull(actividadesDesarrollar));
            registro.setEvidenciaDescripcion(vacioANull(evidenciaDescripcion));
            if (evidenciaArchivo != null && !evidenciaArchivo.isEmpty()) {
                registro.setEvidenciaRutaArchivo(guardarArchivoGenerico(evidenciaArchivo, "evidencias-momento", idEtapa));
            }
            registro.setObservacionAprendiz(vacioANull(observacionAprendiz));
        } else if (numeroMomento == 2) {
            registro.setObservacionAprendiz(vacioANull(observacionAprendiz));
            registro.setFechaMomento(fechaMomento);
            registro.setObservacionEnteCoformador(vacioANull(observacionEnteCoformador));
        } else {
            registro.setRetroAprendizProceso(vacioANull(retroAprendizProceso));
            registro.setRetroAprendizDesempeno(vacioANull(retroAprendizDesempeno));
            registro.setRetroEnteProceso(vacioANull(retroEnteProceso));
            registro.setRetroEnteDesempeno(vacioANull(retroEnteDesempeno));
        }
        registro.setCiudad(vacioANull(ciudad));
        registro.setFechaDiligenciamiento(fechaDiligenciamiento);
        registro.setModalidadFirma(modalidadFirma != null ? modalidadFirma : ModalidadFirma.VIRTUAL);
        registro = evaluacionMomentoRepository.save(registro);

        if (numeroMomento >= 2) {
            sembrarFactoresSiNoExisten(registro);
            guardarFactoresAprendiz(registro, idFactor, valoracionFactor, observacionFactor);
        }

        asignacionInstructorEtapaRepository.findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(idEtapa)
                .ifPresent(asignacion -> notificacionService.crear(asignacion.getInstructor().getUsuario(),
                        "📋 El aprendiz diligenció su parte del Momento " + numeroMomento + " del Formato 023. Ya puedes revisarlo.",
                        "/instructor/seguimiento/planeacion"));

        regenerarSiCompleto(etapa);
    }

    // Guarda la valoración/observación de cada factor (Técnico/Actitudinal) que el aprendiz marcó
    // en su autoevaluación. Los 3 arreglos viajan alineados por índice desde el formulario (ver
    // bitacoras.html: un input oculto "idFactor" + "valoracionFactor" + "observacionFactor" por fila).
    private void guardarFactoresAprendiz(EvaluacionMomento momento, List<Long> idFactor,
            List<String> valoracionFactor, List<String> observacionFactor) {
        if (idFactor == null || idFactor.isEmpty()) {
            return;
        }
        List<FactorMomento> paraGuardar = new ArrayList<>();
        for (int i = 0; i < idFactor.size(); i++) {
            Long id = idFactor.get(i);
            if (id == null) continue;
            FactorMomento factor = factorMomentoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("El factor indicado no existe."));
            // Defensa contra IDOR: el idFactor viaja en un input oculto del formulario, así que hay
            // que verificar que en verdad pertenezca al momento de ESTA etapa antes de modificarlo.
            if (!factor.getEvaluacionMomento().getIdEvaluacionMomento().equals(momento.getIdEvaluacionMomento())) {
                throw new IllegalArgumentException("El factor indicado no pertenece a este momento.");
            }
            String valoracionTexto = valoracionFactor != null && i < valoracionFactor.size() ? valoracionFactor.get(i) : null;
            String observacionTexto = observacionFactor != null && i < observacionFactor.size() ? observacionFactor.get(i) : null;
            factor.setValoracion(valoracionTexto != null && !valoracionTexto.isBlank()
                    ? ValoracionFactor.valueOf(valoracionTexto) : null);
            factor.setObservacion(vacioANull(observacionTexto));
            paraGuardar.add(factor);
        }
        factorMomentoRepository.saveAll(paraGuardar);
    }

    /** El aprendiz digita su correo institucional (distinto de su credencial de acceso) para el 023. */
    @Transactional
    public void guardarCorreoInstitucional(Long idUsuarioAprendiz, Long idEtapa, String correoInstitucional) {
        EtapaProductiva etapa = etapaDelAprendiz(idUsuarioAprendiz, idEtapa);
        etapa.setCorreoInstitucionalAprendiz(vacioANull(correoInstitucional));
        etapaProductivaRepository.save(etapa);
    }

    /** Firmas (imagen) que sube el aprendiz: la suya propia y la del ente co-formador. */
    @Transactional
    public void guardarFirmaAprendiz(Long idUsuarioAprendiz, Long idEtapa, MultipartFile firmaAprendiz, MultipartFile firmaEmpresa) {
        EtapaProductiva etapa = etapaDelAprendiz(idUsuarioAprendiz, idEtapa);
        if (firmaAprendiz != null && !firmaAprendiz.isEmpty()) {
            etapa.setFirmaAprendizRuta(guardarImagen(firmaAprendiz, "firmas", idEtapa));
        }
        if (firmaEmpresa != null && !firmaEmpresa.isEmpty()) {
            etapa.setFirmaEnteCoformadorRuta(guardarImagen(firmaEmpresa, "firmas", idEtapa));
        }
        etapaProductivaRepository.save(etapa);
        // Si el 023 ya se había generado, se regenera para que la firma nueva quede plasmada
        regenerarSiCompleto(etapa);
    }

    /** Firma (imagen) que sube el Instructor de Seguimiento. */
    @Transactional
    public void guardarFirmaInstructor(Long idUsuarioInstructor, Long idEtapa, MultipartFile firma) {
        EtapaProductiva etapa = etapaDelInstructor(idUsuarioInstructor, idEtapa);
        if (firma != null && !firma.isEmpty()) {
            etapa.setFirmaInstructorRuta(guardarImagen(firma, "firmas", idEtapa));
            etapaProductivaRepository.save(etapa);
            // Si el 023 ya se había generado, se regenera para que la firma nueva quede plasmada
            regenerarSiCompleto(etapa);
        }
    }

    private EvaluacionMomento obtenerOCrearMomento(EtapaProductiva etapa, int numeroMomento) {
        return evaluacionMomentoRepository
                .findByEtapaProductivaIdEtapaAndNumeroMomento(etapa.getIdEtapa(), numeroMomento)
                .orElse(EvaluacionMomento.builder()
                        .etapaProductiva(etapa)
                        .numeroMomento(numeroMomento)
                        .build());
    }

    // Crea la fila de EvaluacionMomento (vacía) y siembra sus 13 factores en cuanto el grupo de
    // bitácoras del Momento 2/3 queda ENTREGADA, sin esperar al primer guardado de ningún lado —
    // así el aprendiz ya tiene los idFactor que necesita para autoevaluarse la primera vez que
    // abre el panel. No depende de "anteriorCompleto": sembrar de más es inofensivo porque el
    // formulario sigue oculto en la UI hasta que el momento quede realmente habilitado.
    private void asegurarMomentosSembrados(EtapaProductiva etapa) {
        List<CronogramaBitacoras> cronograma = cronogramaBitacorasRepository
                .findByEtapaProductivaIdEtapaOrderByNumeroBitacoraAsc(etapa.getIdEtapa());
        for (int numero = 2; numero <= TOTAL_MOMENTOS; numero++) {
            int desde = (numero - 1) * BITACORAS_POR_MOMENTO + 1;
            int hasta = numero * BITACORAS_POR_MOMENTO;
            if (!bitacorasEntregadas(cronograma, desde, hasta)) {
                continue;
            }
            int numeroFinal = numero;
            EvaluacionMomento actual = evaluacionMomentoRepository
                    .findByEtapaProductivaIdEtapaAndNumeroMomento(etapa.getIdEtapa(), numero)
                    .orElseGet(() -> evaluacionMomentoRepository.save(
                            EvaluacionMomento.builder().etapaProductiva(etapa).numeroMomento(numeroFinal).build()));
            sembrarFactoresSiNoExisten(actual);
        }
    }

    // Si con el último guardado (de cualquiera de los 2 lados) los 3 momentos quedan completos
    // por aprendiz E instructor, genera/regenera el PDF y evalúa la certificación automática.
    private void regenerarSiCompleto(EtapaProductiva etapa) {
        List<MomentoEstadoDto> momentos = construirEstadoMomentos(etapa);
        boolean todosCompletos = momentos.stream().allMatch(m -> m.isAprendizCompleto() && m.isInstructorCompleto());
        // El PDF solo se genera (y se evalúa certificación) si el instructor aprobó el Momento 3 —
        // si quedó "No aprobado", el aprendiz no recibe un Formato 023 descargable.
        boolean juicioAprobado = "APROBADO".equals(momentos.get(TOTAL_MOMENTOS - 1).getJuicioEvaluacion());
        if (todosCompletos && juicioAprobado) {
            generarFormato023(etapa);
            verificarYTransicionarPorCertificar(etapa);
        }
    }

    // Siembra las 13 filas de Factores Técnicos/Actitudinales (Momento 2 y 3), en blanco, la
    // primera vez que el momento se toca — mismo criterio "crear si no existe" que ya usa
    // ConfiguracionGlobalService. Rellenarlas fila por fila queda para una fase futura.
    private void sembrarFactoresSiNoExisten(EvaluacionMomento momento) {
        if (!factorMomentoRepository
                .findByEvaluacionMomentoIdEvaluacionMomentoOrderByIdFactorAsc(momento.getIdEvaluacionMomento()).isEmpty()) {
            return;
        }
        List<FactorMomento> nuevos = new ArrayList<>();
        for (String nombre : FACTORES_TECNICOS) {
            nuevos.add(FactorMomento.builder().evaluacionMomento(momento).categoria(CategoriaFactor.TECNICO).nombreVariable(nombre).build());
        }
        for (String nombre : FACTORES_ACTITUDINALES) {
            nuevos.add(FactorMomento.builder().evaluacionMomento(momento).categoria(CategoriaFactor.ACTITUDINAL).nombreVariable(nombre).build());
        }
        factorMomentoRepository.saveAll(nuevos);
    }

    // Estado de los 3 momentos: habilitado (bitácoras del grupo entregadas + momento anterior
    // completo por ambos lados) y, por cada lado, si ya diligenció su parte.
    private List<MomentoEstadoDto> construirEstadoMomentos(EtapaProductiva etapa) {
        List<CronogramaBitacoras> cronograma = cronogramaBitacorasRepository
                .findByEtapaProductivaIdEtapaOrderByNumeroBitacoraAsc(etapa.getIdEtapa());
        Map<Integer, EvaluacionMomento> porNumero = new HashMap<>();
        for (EvaluacionMomento m : evaluacionMomentoRepository
                .findByEtapaProductivaIdEtapaOrderByNumeroMomentoAsc(etapa.getIdEtapa())) {
            porNumero.put(m.getNumeroMomento(), m);
        }

        List<MomentoEstadoDto> momentos = new ArrayList<>();
        boolean anteriorCompleto = true; // el Momento 1 no depende de ningún anterior
        for (int numero = 1; numero <= TOTAL_MOMENTOS; numero++) {
            int desde = (numero - 1) * BITACORAS_POR_MOMENTO + 1;
            int hasta = numero * BITACORAS_POR_MOMENTO;
            boolean bitacorasListas = bitacorasEntregadas(cronograma, desde, hasta);
            EvaluacionMomento actual = porNumero.get(numero);

            boolean factoresCompletos = numero >= 2 && todosFactoresValorados(actual);
            boolean aprendizCompleto = actual != null && switch (numero) {
                case 1 -> noVacio(actual.getCompetenciasDesarrollar()) && noVacio(actual.getResultadosAprendizaje())
                        && noVacio(actual.getActividadesDesarrollar());
                case 2 -> noVacio(actual.getObservacionAprendiz()) && factoresCompletos;
                default -> noVacio(actual.getRetroAprendizProceso()) && noVacio(actual.getRetroAprendizDesempeno())
                        && factoresCompletos;
            };
            boolean instructorCompleto = actual != null && (numero == TOTAL_MOMENTOS
                    ? noVacio(actual.getRetroInstructorProceso()) && noVacio(actual.getRetroInstructorDesempeno())
                            && actual.getJuicioEvaluacion() != null
                    : noVacio(actual.getObservacion()));

            momentos.add(MomentoEstadoDto.builder()
                    .numero(numero)
                    .habilitado(bitacorasListas && anteriorCompleto)
                    .aprendizCompleto(aprendizCompleto)
                    .instructorCompleto(instructorCompleto)
                    .bitacoraDesde(desde)
                    .bitacoraHasta(hasta)
                    .observacion(actual != null ? actual.getObservacion() : null)
                    .juicioEvaluacion(actual != null && actual.getJuicioEvaluacion() != null ? actual.getJuicioEvaluacion().name() : null)
                    .fechaRegistro(actual != null ? actual.getFechaRegistro().format(FORMATO_FECHA_HORA) : null)
                    .retroInstructorProceso(actual != null ? actual.getRetroInstructorProceso() : null)
                    .retroInstructorDesempeno(actual != null ? actual.getRetroInstructorDesempeno() : null)
                    .observacionAprendiz(actual != null ? actual.getObservacionAprendiz() : null)
                    .competenciasDesarrollar(actual != null ? actual.getCompetenciasDesarrollar() : null)
                    .resultadosAprendizaje(actual != null ? actual.getResultadosAprendizaje() : null)
                    .actividadesDesarrollar(actual != null ? actual.getActividadesDesarrollar() : null)
                    .evidenciaDescripcion(actual != null ? actual.getEvidenciaDescripcion() : null)
                    .evidenciaRutaArchivo(actual != null ? actual.getEvidenciaRutaArchivo() : null)
                    .fechaMomento(actual != null && actual.getFechaMomento() != null
                            ? actual.getFechaMomento().format(FORMATO_FECHA_ISO) : null)
                    .observacionEnteCoformador(actual != null ? actual.getObservacionEnteCoformador() : null)
                    .retroEnteProceso(actual != null ? actual.getRetroEnteProceso() : null)
                    .retroEnteDesempeno(actual != null ? actual.getRetroEnteDesempeno() : null)
                    .retroAprendizProceso(actual != null ? actual.getRetroAprendizProceso() : null)
                    .retroAprendizDesempeno(actual != null ? actual.getRetroAprendizDesempeno() : null)
                    .factoresTecnicos(numero >= 2 ? mapFactores(actual, CategoriaFactor.TECNICO) : List.of())
                    .factoresActitudinales(numero >= 2 ? mapFactores(actual, CategoriaFactor.ACTITUDINAL) : List.of())
                    .enlaceGrabacion(actual != null ? actual.getEnlaceGrabacion() : null)
                    .ciudad(actual != null ? actual.getCiudad() : null)
                    .fechaDiligenciamiento(actual != null && actual.getFechaDiligenciamiento() != null
                            ? actual.getFechaDiligenciamiento().format(FORMATO_FECHA_ISO) : null)
                    .modalidadFirma(actual != null && actual.getModalidadFirma() != null ? actual.getModalidadFirma().name() : "VIRTUAL")
                    .build());

            anteriorCompleto = aprendizCompleto && instructorCompleto;
        }
        return momentos;
    }

    // Todos los 13 factores (8 Técnicos + 5 Actitudinales) del momento ya tienen una valoración
    // marcada. Si el momento ni siquiera se ha sembrado (actual == null) o no tiene factores
    // todavía, se considera incompleto.
    private boolean todosFactoresValorados(EvaluacionMomento momento) {
        if (momento == null || momento.getIdEvaluacionMomento() == null) {
            return false;
        }
        List<FactorMomento> factores = factorMomentoRepository
                .findByEvaluacionMomentoIdEvaluacionMomentoOrderByIdFactorAsc(momento.getIdEvaluacionMomento());
        return !factores.isEmpty() && factores.stream().allMatch(f -> f.getValoracion() != null);
    }

    private List<FactorEstadoDto> mapFactores(EvaluacionMomento momento, CategoriaFactor categoria) {
        return obtenerFactores(momento, categoria).stream()
                .map(f -> FactorEstadoDto.builder()
                        .idFactor(f.getIdFactor())
                        .nombreVariable(f.getNombreVariable())
                        .valoracion(f.getValoracion() != null ? f.getValoracion().name() : null)
                        .observacion(f.getObservacion())
                        .build())
                .toList();
    }

    private boolean bitacorasEntregadas(List<CronogramaBitacoras> cronograma, int desde, int hasta) {
        List<CronogramaBitacoras> grupo = cronograma.stream()
                .filter(c -> c.getNumeroBitacora() >= desde && c.getNumeroBitacora() <= hasta)
                .toList();
        return grupo.size() == (hasta - desde + 1)
                && grupo.stream().allMatch(c -> c.getEstado() == EstadoBitacora.ENTREGADA);
    }

    /** 📄 Genera el PDF del Formato 023 y lo guarda como FormatoPlaneacion. */
    private void generarFormato023(EtapaProductiva etapa) {
        try {
            byte[] pdf = generadorFormato023HtmlService.generarPdf(etapa);
            Path directorio = Paths.get(uploadRootDir, "formato-planeacion", "etapa_" + etapa.getIdEtapa());
            Files.createDirectories(directorio);
            String nombreArchivo = "formato_023_" + LocalDateTime.now().format(SELLO_TIEMPO) + ".pdf";
            Path destino = directorio.resolve(nombreArchivo);
            Files.write(destino, pdf);
            String rutaGuardada = "/" + destino.toString().replace('\\', '/');

            FormatoPlaneacion formato = formatoPlaneacionRepository.findByEtapaProductivaIdEtapa(etapa.getIdEtapa())
                    .orElse(FormatoPlaneacion.builder()
                            .etapaProductiva(etapa)
                            .asunto("Formato 023 generado automáticamente")
                            .build());
            formato.setRutaArchivo(rutaGuardada);
            formato.setFechaEntrega(LocalDate.now());
            formato.setFechaHoraSubida(LocalDateTime.now());
            formatoPlaneacionRepository.save(formato);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el Formato 023: " + e.getMessage(), e);
        }
    }

    /**
     * 📄 Plus para el Instructor de Seguimiento: descarga el PDF de un solo momento (1, 2 o 3)
     * apenas quede completo por ambos lados (aprendiz e instructor), sin esperar a que los 3
     * estén listos para el Formato 023 completo. Mismo encabezado general, solo la tabla de
     * ese momento.
     */
    @Transactional(readOnly = true)
    public byte[] descargarMomentoPdf(Long idUsuarioInstructor, Long idEtapa, int numeroMomento) {
        if (numeroMomento < 1 || numeroMomento > TOTAL_MOMENTOS) {
            throw new IllegalArgumentException("El momento indicado no es válido.");
        }
        EtapaProductiva etapa = etapaDelInstructor(idUsuarioInstructor, idEtapa);

        List<MomentoEstadoDto> momentos = construirEstadoMomentos(etapa);
        MomentoEstadoDto momento = momentos.get(numeroMomento - 1);
        if (!momento.isAprendizCompleto() || !momento.isInstructorCompleto()) {
            throw new IllegalStateException("El Momento " + numeroMomento
                    + " todavía no está completo por ambos lados (aprendiz e instructor).");
        }

        try {
            return generadorFormato023HtmlService.generarPdfMomento(etapa, numeroMomento);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el PDF del Momento " + numeroMomento + ": " + e.getMessage(), e);
        }
    }

    // ────────────────────────────── Construcción del PDF (GFPI-F-023) ──────────────────────────────

    private byte[] construirPdf023(EtapaProductiva etapa) {
        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
        Ficha ficha = etapa.getAprendizFicha().getFicha();

        Map<Integer, EvaluacionMomento> porNumero = new HashMap<>();
        for (EvaluacionMomento m : evaluacionMomentoRepository
                .findByEtapaProductivaIdEtapaOrderByNumeroMomentoAsc(etapa.getIdEtapa())) {
            porNumero.put(m.getNumeroMomento(), m);
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        com.lowagie.text.Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(5, 112, 21));
        com.lowagie.text.Font fSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        com.lowagie.text.Font fEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        com.lowagie.text.Font fValor = FontFactory.getFont(FontFactory.HELVETICA, 9);
        com.lowagie.text.Font fSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(5, 112, 21));
        com.lowagie.text.Font fSubseccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);

        Paragraph titulo = new Paragraph(
                "GFPI-F-023 — FORMATO DE PLANEACIÓN, SEGUIMIENTO Y EVALUACIÓN DE LA ETAPA PRÁCTICA", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph generado = new Paragraph("Generado automáticamente por KRONOS el " + LocalDate.now().format(FORMATO_FECHA),
                fSubtitulo);
        generado.setAlignment(Element.ALIGN_CENTER);
        generado.setSpacingAfter(12);
        documento.add(generado);

        // 1) Información general
        agregarSeccion(documento, "INFORMACIÓN GENERAL", fSeccion);
        PdfPTable general = nuevaTablaDatos();
        agregarFilaDatos(general, "Regional", REGIONAL_PREDETERMINADA, fEtiqueta, fValor);
        agregarFilaDatos(general, "Centro de formación", CENTRO_PREDETERMINADO, fEtiqueta, fValor);
        agregarFilaDatos(general, "Nivel formativo", ficha.getProgramaFormacion().getNivelFormacion().getNombreNivel(), fEtiqueta, fValor);
        agregarFilaDatos(general, "Programa de formación", ficha.getProgramaFormacion().getNombrePrograma(), fEtiqueta, fValor);
        agregarFilaDatos(general, "No. Grupo (Ficha)", ficha.getNumeroFicha(), fEtiqueta, fValor);
        agregarFilaDatos(general, "Modalidad de formación", etiquetaModalidad(etapa.getModalidad()), fEtiqueta, fValor);
        agregarFilaDatos(general, "Estrategia formativa", ESTRATEGIA_PREDETERMINADA, fEtiqueta, fValor);
        agregarFilaDatos(general, "Fecha fin etapa lectiva",
                fecha(ficha.getFechaHabilitacionEtapaPractica().minusDays(1)), fEtiqueta, fValor);
        general.setSpacingAfter(14);
        documento.add(general);

        // 2) Datos del aprendiz
        agregarSeccion(documento, "DATOS DEL APRENDIZ", fSeccion);
        PdfPTable datosAprendiz = nuevaTablaDatos();
        agregarFilaDatos(datosAprendiz, "Nombre completo", aprendiz.getNombre() + " " + aprendiz.getApellido(), fEtiqueta, fValor);
        agregarFilaDatos(datosAprendiz, "Tipo de documento", aprendiz.getTipoDocumento() != null ? aprendiz.getTipoDocumento().name() : SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(datosAprendiz, "N° de identificación", valor(aprendiz.getDocumento()), fEtiqueta, fValor);
        agregarFilaDatos(datosAprendiz, "Contacto telefónico", valor(aprendiz.getTelefono()), fEtiqueta, fValor);
        agregarFilaDatos(datosAprendiz, "Dirección", SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(datosAprendiz, "Correo electrónico personal", valor(aprendiz.getCorreoElectronico()), fEtiqueta, fValor);
        agregarFilaDatos(datosAprendiz, "Correo electrónico institucional", valor(etapa.getCorreoInstitucionalAprendiz()), fEtiqueta, fValor);
        agregarFilaDatos(datosAprendiz, "Alternativa de etapa productiva", etapa.getTipoContrato().getNombreTipoContrato(), fEtiqueta, fValor);
        agregarFilaDatos(datosAprendiz, "Fecha de registro en SofiaPlus",
                etapa.getFechaCreacion() != null ? etapa.getFechaCreacion().toLocalDate().format(FORMATO_FECHA) : SIN_DATO, fEtiqueta, fValor);
        datosAprendiz.setSpacingAfter(14);
        documento.add(datosAprendiz);

        // 3) Datos del instructor de seguimiento
        agregarSeccion(documento, "DATOS DEL INSTRUCTOR DE SEGUIMIENTO", fSeccion);
        PdfPTable datosInstructor = nuevaTablaDatos();
        Usuario instructorUsuario = asignacionInstructorEtapaRepository
                .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(etapa.getIdEtapa())
                .map(a -> a.getInstructor().getUsuario())
                .orElse(null);
        agregarFilaDatos(datosInstructor, "Nombre",
                instructorUsuario != null ? instructorUsuario.getNombre() + " " + instructorUsuario.getApellido() : SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(datosInstructor, "Contacto telefónico",
                instructorUsuario != null ? valor(instructorUsuario.getTelefono()) : SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(datosInstructor, "Correo electrónico institucional", SIN_DATO, fEtiqueta, fValor);
        datosInstructor.setSpacingAfter(14);
        documento.add(datosInstructor);

        // 4) Datos del ente co-formador
        agregarSeccion(documento, "DATOS DEL ENTE CO-FORMADOR", fSeccion);
        PdfPTable datosEmpresa = nuevaTablaDatos();
        agregarFilaDatos(datosEmpresa, "Nombre empresa o entidad", etapa.getEmpresa().getNombreEmpresa(), fEtiqueta, fValor);
        agregarFilaDatos(datosEmpresa, "Dirección", valor(etapa.getEmpresa().getDireccion()), fEtiqueta, fValor);
        agregarFilaDatos(datosEmpresa, "NIT", valor(etapa.getEmpresa().getNit()), fEtiqueta, fValor);
        agregarFilaDatos(datosEmpresa, "Correo electrónico", valor(etapa.getEmpresa().getCorreo()), fEtiqueta, fValor);
        agregarFilaDatos(datosEmpresa, "Jefe inmediato / tutor", valor(etapa.getNombreJefeInmediato()), fEtiqueta, fValor);
        agregarFilaDatos(datosEmpresa, "Contacto telefónico jefe", valor(etapa.getTelefonoJefeInmediato()), fEtiqueta, fValor);
        datosEmpresa.setSpacingAfter(14);
        documento.add(datosEmpresa);

        // 5) Persona en situación de discapacidad (fuera de alcance de este pase)
        agregarSeccion(documento, "PERSONA EN SITUACIÓN DE DISCAPACIDAD (SI APLICA)", fSeccion);
        Paragraph discapacidad = new Paragraph("Sin diligenciar.", fValor);
        discapacidad.setSpacingAfter(14);
        documento.add(discapacidad);

        String fechaArl = buscarFechaAfiliacionArl(etapa.getIdEtapa());
        long visitasRealizadas = visitaSeguimientoRepository
                .findByEtapaProductivaIdEtapaOrderByFechaVisitaDesc(etapa.getIdEtapa()).stream()
                .filter(v -> v.getEstadoVisita() == EstadoVisita.REALIZADA)
                .count();

        // 6) Momento 1
        EvaluacionMomento m1 = porNumero.get(1);
        agregarSeccion(documento, ETIQUETAS_MOMENTO[0], fSeccion);
        PdfPTable datosM1 = nuevaTablaDatos();
        agregarFilaDatos(datosM1, "Fecha inicio etapa productiva", fecha(etapa.getFechaInicio()), fEtiqueta, fValor);
        agregarFilaDatos(datosM1, "Fecha fin etapa productiva", fecha(etapa.getFechaFin()), fEtiqueta, fValor);
        agregarFilaDatos(datosM1, "Fecha de afiliación a la ARL", fechaArl, fEtiqueta, fValor);
        agregarFilaDatos(datosM1, "Número de póliza ARL", SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(datosM1, "Horario", ficha.getJornada() != null ? ficha.getJornada().name() : SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(datosM1, "Enlace de grabación", m1 != null ? valor(m1.getEnlaceGrabacion()) : SIN_DATO, fEtiqueta, fValor);
        datosM1.setSpacingAfter(8);
        documento.add(datosM1);

        agregarSubtitulo(documento, "Concertación plan de trabajo durante la etapa productiva", fSubseccion);
        documento.add(parrafoCampo("Competencias a desarrollar", m1 != null ? m1.getCompetenciasDesarrollar() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Resultados de aprendizaje", m1 != null ? m1.getResultadosAprendizaje() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Actividades a desarrollar", m1 != null ? m1.getActividadesDesarrollar() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Evidencias de aprendizaje", m1 != null ? m1.getEvidenciaDescripcion() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Observaciones adicionales", m1 != null ? m1.getObservacionAprendiz() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Observación del instructor de seguimiento", m1 != null ? m1.getObservacion() : null, fEtiqueta, fValor));

        agregarBloqueFirmasYCierre(documento, etapa, m1, fEtiqueta, fValor, fSubtitulo);

        // 7) Momento 2
        EvaluacionMomento m2 = porNumero.get(2);
        agregarSeccion(documento, ETIQUETAS_MOMENTO[1], fSeccion);
        agregarFechaModalidadSeguimiento(documento, m2, fEtiqueta, fValor);
        agregarTablaFactores(documento, "Factores Técnicos", obtenerFactores(m2, CategoriaFactor.TECNICO), fSubseccion, fEtiqueta, fValor);
        agregarTablaFactores(documento, "Factores Actitudinales y Comportamentales", obtenerFactores(m2, CategoriaFactor.ACTITUDINAL), fSubseccion, fEtiqueta, fValor);
        documento.add(parrafoCampo("Observaciones complementarias del instructor de seguimiento", m2 != null ? m2.getObservacion() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Observaciones del aprendiz", m2 != null ? m2.getObservacionAprendiz() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Observaciones del responsable ente co-formador", m2 != null ? m2.getObservacionEnteCoformador() : null, fEtiqueta, fValor));
        agregarBloqueFirmasYCierre(documento, etapa, m2, fEtiqueta, fValor, fSubtitulo);

        // 8) Momento 3
        EvaluacionMomento m3 = porNumero.get(3);
        agregarSeccion(documento, ETIQUETAS_MOMENTO[2], fSeccion);
        PdfPTable datosM3 = nuevaTablaDatos();
        agregarFilaDatos(datosM3, "Fecha inicio etapa productiva", fecha(etapa.getFechaInicio()), fEtiqueta, fValor);
        agregarFilaDatos(datosM3, "Fecha fin ejecución etapa productiva", fecha(etapa.getFechaFin()), fEtiqueta, fValor);
        agregarFilaDatos(datosM3, "Número de visitas realizadas", String.valueOf(visitasRealizadas), fEtiqueta, fValor);
        agregarFilaDatos(datosM3, "Evaluación realizada en forma",
                m3 != null && m3.getModalidadFirma() != null ? etiquetaModalidadFirma(m3.getModalidadFirma()) : SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(datosM3, "Enlace de grabación", m3 != null ? valor(m3.getEnlaceGrabacion()) : SIN_DATO, fEtiqueta, fValor);
        datosM3.setSpacingAfter(8);
        documento.add(datosM3);
        agregarTablaFactores(documento, "Factores Técnicos", obtenerFactores(m3, CategoriaFactor.TECNICO), fSubseccion, fEtiqueta, fValor);
        agregarTablaFactores(documento, "Factores Actitudinales y Comportamentales", obtenerFactores(m3, CategoriaFactor.ACTITUDINAL), fSubseccion, fEtiqueta, fValor);

        agregarSubtitulo(documento, "Retroalimentación ente co-formador", fSubseccion);
        documento.add(parrafoCampo("Proceso de formación del aprendiz", m3 != null ? m3.getRetroEnteProceso() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Desempeño de las competencias técnicas y actitudinales", m3 != null ? m3.getRetroEnteDesempeno() : null, fEtiqueta, fValor));

        agregarSubtitulo(documento, "Retroalimentación instructor de seguimiento", fSubseccion);
        documento.add(parrafoCampo("Proceso de formación del aprendiz", m3 != null ? m3.getRetroInstructorProceso() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Desempeño de las competencias técnicas y actitudinales", m3 != null ? m3.getRetroInstructorDesempeno() : null, fEtiqueta, fValor));

        agregarSubtitulo(documento, "Retroalimentación del aprendiz", fSubseccion);
        documento.add(parrafoCampo("Proceso de formación del aprendiz", m3 != null ? m3.getRetroAprendizProceso() : null, fEtiqueta, fValor));
        documento.add(parrafoCampo("Desempeño de las competencias técnicas y actitudinales", m3 != null ? m3.getRetroAprendizDesempeno() : null, fEtiqueta, fValor));

        Paragraph juicio = new Paragraph("Juicio de evaluación de la etapa productiva: "
                + (m3 != null && m3.getJuicioEvaluacion() == JuicioEvaluacion.APROBADO ? "☑ Aprobado   ☐ No aprobado"
                : m3 != null && m3.getJuicioEvaluacion() == JuicioEvaluacion.NO_APROBADO ? "☐ Aprobado   ☑ No aprobado"
                : "☐ Aprobado   ☐ No aprobado"), fEtiqueta);
        juicio.setSpacingBefore(4);
        juicio.setSpacingAfter(8);
        documento.add(juicio);

        agregarBloqueFirmasYCierre(documento, etapa, m3, fEtiqueta, fValor, fSubtitulo);

        documento.close();
        return salida.toByteArray();
    }

    private List<FactorMomento> obtenerFactores(EvaluacionMomento momento, CategoriaFactor categoria) {
        if (momento == null || momento.getIdEvaluacionMomento() == null) {
            return List.of();
        }
        return factorMomentoRepository.findByEvaluacionMomentoIdEvaluacionMomentoOrderByIdFactorAsc(momento.getIdEvaluacionMomento())
                .stream().filter(f -> f.getCategoria() == categoria).toList();
    }

    private void agregarFechaModalidadSeguimiento(Document documento, EvaluacionMomento momento,
            com.lowagie.text.Font fEtiqueta, com.lowagie.text.Font fValor) {
        PdfPTable tabla = nuevaTablaDatos();
        agregarFilaDatos(tabla, "Fecha del momento de seguimiento",
                momento != null && momento.getFechaMomento() != null ? fecha(momento.getFechaMomento()) : SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(tabla, "Modalidad del seguimiento",
                momento != null && momento.getModalidadFirma() != null ? etiquetaModalidadFirma(momento.getModalidadFirma()) : SIN_DATO, fEtiqueta, fValor);
        agregarFilaDatos(tabla, "Enlace de grabación", momento != null ? valor(momento.getEnlaceGrabacion()) : SIN_DATO, fEtiqueta, fValor);
        tabla.setSpacingAfter(8);
        documento.add(tabla);
    }

    private void agregarTablaFactores(Document documento, String titulo, List<FactorMomento> factores,
            com.lowagie.text.Font fSubseccion, com.lowagie.text.Font fEtiqueta, com.lowagie.text.Font fValor) {
        Paragraph t = new Paragraph(titulo, fSubseccion);
        t.setSpacingBefore(4);
        documento.add(t);

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        try {
            tabla.setWidths(new float[]{2.4f, 1f, 1f, 2.6f});
        } catch (Exception ignored) {
            // ancho por defecto si el arreglo no aplica
        }
        for (String encabezado : new String[]{"Variable", "Satisfactorio", "Por mejorar", "Observaciones / Compromisos de mejora"}) {
            PdfPCell celda = new PdfPCell(new Paragraph(encabezado, fEtiqueta));
            celda.setBackgroundColor(new Color(245, 247, 245));
            celda.setPadding(3);
            tabla.addCell(celda);
        }
        for (FactorMomento f : factores) {
            tabla.addCell(celdaTexto(f.getNombreVariable(), fValor, Element.ALIGN_LEFT));
            tabla.addCell(celdaTexto(f.getValoracion() == ValoracionFactor.SATISFACTORIO ? "X" : "", fValor, Element.ALIGN_CENTER));
            tabla.addCell(celdaTexto(f.getValoracion() == ValoracionFactor.POR_MEJORAR ? "X" : "", fValor, Element.ALIGN_CENTER));
            tabla.addCell(celdaTexto(f.getObservacion() != null ? f.getObservacion() : "", fValor, Element.ALIGN_LEFT));
        }
        tabla.setSpacingAfter(10);
        documento.add(tabla);
    }

    private void agregarBloqueFirmasYCierre(Document documento, EtapaProductiva etapa, EvaluacionMomento momento,
            com.lowagie.text.Font fEtiqueta, com.lowagie.text.Font fValor, com.lowagie.text.Font fSubtitulo) {
        PdfPTable firmas = new PdfPTable(3);
        firmas.setWidthPercentage(100);
        firmas.setSpacingBefore(6);
        agregarCeldaFirma(firmas, "Firma del aprendiz", etapa.getFirmaAprendizRuta(), fEtiqueta, fSubtitulo);
        agregarCeldaFirma(firmas, "Firma del instructor de seguimiento", etapa.getFirmaInstructorRuta(), fEtiqueta, fSubtitulo);
        agregarCeldaFirma(firmas, "Firma del ente co-formador", etapa.getFirmaEnteCoformadorRuta(), fEtiqueta, fSubtitulo);
        firmas.setSpacingAfter(6);
        documento.add(firmas);

        String modalidad = momento != null && momento.getModalidadFirma() != null ? etiquetaModalidadFirma(momento.getModalidadFirma()) : "Virtual";
        Paragraph cierre = new Paragraph(
                "Ciudad: " + (momento != null ? valor(momento.getCiudad()) : SIN_DATO)
                        + "   ·   Fecha de diligenciamiento: " + (momento != null && momento.getFechaDiligenciamiento() != null
                        ? momento.getFechaDiligenciamiento() : SIN_DATO)
                        + "   ·   Modalidad: " + modalidad,
                fValor);
        cierre.setSpacingAfter(16);
        documento.add(cierre);
    }

    private void agregarCeldaFirma(PdfPTable tabla, String etiqueta, String rutaWeb,
            com.lowagie.text.Font fEtiqueta, com.lowagie.text.Font fSubtitulo) {
        PdfPCell celda = new PdfPCell();
        celda.setPadding(4);
        celda.addElement(new Paragraph(etiqueta, fEtiqueta));
        if (rutaWeb != null) {
            try {
                Image img = Image.getInstance(resolverRutaFisica(rutaWeb));
                img.scaleToFit(110, 45);
                celda.addElement(img);
            } catch (Exception e) {
                celda.addElement(new Paragraph("(firma no disponible)", fSubtitulo));
            }
        } else {
            celda.addElement(new Paragraph("_____________________", fSubtitulo));
        }
        tabla.addCell(celda);
    }

    private String resolverRutaFisica(String rutaWeb) {
        String relativa = rutaWeb.startsWith("/") ? rutaWeb.substring(1) : rutaWeb;
        return Paths.get(relativa).toAbsolutePath().toString();
    }

    private String buscarFechaAfiliacionArl(Long idEtapa) {
        return documentoRequisitoRepository.findByEtapaProductivaIdEtapa(idEtapa).stream()
                .filter(d -> d.getPlantillaFormato() != null && d.getPlantillaFormato().getNombreDocumento() != null
                        && d.getPlantillaFormato().getNombreDocumento().toUpperCase().contains("ARL"))
                .map(DocumentoRequisito::getFechaSubida)
                .filter(f -> f != null)
                .map(f -> f.toLocalDate().format(FORMATO_FECHA))
                .findFirst()
                .orElse(SIN_DATO);
    }

    private PdfPTable nuevaTablaDatos() {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        try {
            tabla.setWidths(new float[]{1f, 2f});
        } catch (Exception ignored) {
            // ancho por defecto si el arreglo no aplica
        }
        return tabla;
    }

    private void agregarSeccion(Document documento, String titulo, com.lowagie.text.Font fuente) {
        Paragraph seccion = new Paragraph(titulo, fuente);
        seccion.setSpacingBefore(10);
        seccion.setSpacingAfter(4);
        documento.add(seccion);
    }

    private void agregarSubtitulo(Document documento, String texto, com.lowagie.text.Font fuente) {
        Paragraph p = new Paragraph(texto, fuente);
        p.setSpacingBefore(4);
        p.setSpacingAfter(4);
        documento.add(p);
    }

    private Paragraph parrafoCampo(String etiqueta, String valorTexto, com.lowagie.text.Font fEtiqueta, com.lowagie.text.Font fValor) {
        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk(etiqueta + ": ", fEtiqueta));
        p.add(new com.lowagie.text.Chunk(valorTexto != null && !valorTexto.isBlank() ? valorTexto : SIN_DATO, fValor));
        p.setSpacingAfter(6);
        return p;
    }

    private PdfPCell celdaTexto(String texto, com.lowagie.text.Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Paragraph(texto != null ? texto : "", fuente));
        celda.setPadding(3);
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    private void agregarFilaDatos(PdfPTable tabla, String etiqueta, String valor,
                                   com.lowagie.text.Font fuenteEtiqueta, com.lowagie.text.Font fuenteValor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Paragraph(etiqueta, fuenteEtiqueta));
        celdaEtiqueta.setPadding(4);
        celdaEtiqueta.setBackgroundColor(new Color(245, 247, 245));
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Paragraph(valor != null ? valor : SIN_DATO, fuenteValor));
        celdaValor.setPadding(4);
        tabla.addCell(celdaValor);
    }

    private String etiquetaModalidad(ModalidadEtapa modalidad) {
        if (modalidad == null) return SIN_DATO;
        return switch (modalidad) {
            case PRESENCIAL -> "Presencial";
            case REMOTO -> "Virtual";
            case HIBRIDO -> "Presencial y Virtual";
        };
    }

    private String etiquetaModalidadFirma(ModalidadFirma modalidad) {
        return modalidad == ModalidadFirma.PRESENCIAL ? "Presencial" : "Virtual";
    }

    // ─────────────────────────────── Certificación ───────────────────────────────

    /**
     * 📊 Resumen de requisitos de certificación de una Etapa Productiva: cuántas bitácoras
     * quedaron aprobadas (de las generadas en su cronograma), si el Formato de Planeación
     * (023) fue aprobado, y cuántas visitas de seguimiento se realizaron.
     */
    @Transactional(readOnly = true)
    public CertificacionInfoDto calcularInfoCertificacion(EtapaProductiva etapa) {
        List<CronogramaBitacoras> cronograma = cronogramaBitacorasRepository.findByEtapaProductivaIdEtapaOrderByNumeroBitacoraAsc(etapa.getIdEtapa());
        List<Bitacora> bitacoras = bitacoraRepository.findByEtapaProductivaIdEtapaOrderByFechaEntregaDesc(etapa.getIdEtapa());

        // Última bitácora radicada por cada cupo del cronograma (por si hubo corrección/resubida)
        Map<Long, Bitacora> ultimaPorCronograma = new HashMap<>();
        for (Bitacora b : bitacoras) {
            Long idCronograma = b.getCronogramaBitacora().getIdCronograma();
            Bitacora actual = ultimaPorCronograma.get(idCronograma);
            if (actual == null || b.getFechaHoraSubida().isAfter(actual.getFechaHoraSubida())) {
                ultimaPorCronograma.put(idCronograma, b);
            }
        }

        int aprobadas = 0;
        for (CronogramaBitacoras cupo : cronograma) {
            Bitacora ultima = ultimaPorCronograma.get(cupo.getIdCronograma());
            if (ultima == null) continue;
            EvaluacionBitacora ev = evaluacionBitacoraRepository
                    .findTopByBitacoraIdBitacoraOrderByFechaEvaluacionDesc(ultima.getIdBitacora()).orElse(null);
            if (ev != null && ev.getResultado() == ResultadoEvaluacion.APROBADO) aprobadas++;
        }

        // El 023 se considera completo cuando los 3 momentos quedaron listos por ambos lados
        // (aprendiz + instructor); ver construirEstadoMomentos.
        boolean formatoAprobado = construirEstadoMomentos(etapa).stream()
                .allMatch(m -> m.isAprendizCompleto() && m.isInstructorCompleto());

        List<VisitaSeguimiento> visitas = visitaSeguimientoRepository.findByEtapaProductivaIdEtapaOrderByFechaVisitaDesc(etapa.getIdEtapa());
        long realizadas = visitas.stream().filter(v -> v.getEstadoVisita() == EstadoVisita.REALIZADA).count();

        return CertificacionInfoDto.builder()
                .totalBitacoras(cronograma.size())
                .bitacorasAprobadas(aprobadas)
                .formatoAprobado(formatoAprobado)
                .visitasRealizadas((int) realizadas)
                .totalVisitas(visitas.size())
                .build();
    }

    /**
     * 🥳 Si la Etapa Productiva completó el 100% de sus bitácoras y el Formato de Planeación
     * (023) fue aprobado, la pasa automáticamente de EN_PROGRESO a TERMINADO (ya no pasa por
     * POR_CERTIFICAR: la certificación oficial no es un proceso de KRONOS, ocurre en Sofía
     * Plus), refleja el mismo cierre en la matrícula del aprendiz (AprendizFicha) y notifica
     * al aprendiz y a todos los Gestores de Etapa activos.
     */
    private void verificarYTransicionarPorCertificar(EtapaProductiva etapa) {
        if (etapa.getEstadoEtapa() != EstadoEtapa.EN_PROGRESO) {
            return;
        }

        CertificacionInfoDto info = calcularInfoCertificacion(etapa);
        if (!info.isBitacorasCompletas() || !info.isFormatoAprobado()) {
            return;
        }

        etapa.setEstadoEtapa(EstadoEtapa.TERMINADO);
        etapa.setFechaTerminacion(java.time.LocalDateTime.now());
        etapaProductivaRepository.save(etapa);

        AprendizFicha aprendizFicha = etapa.getAprendizFicha();
        aprendizFicha.setEstadoAcademico(EstadoAcademico.CERTIFICADO);
        aprendizFichaRepository.save(aprendizFicha);

        Usuario aprendiz = aprendizFicha.getUsuario();
        notificacionService.crear(aprendiz,
                "🎉 ¡Completaste el 100% de tu Etapa Productiva! Tu Instructor de Seguimiento aprobó tus bitácoras "
                        + "y el Formato 023, así que tu Etapa Productiva ya quedó TERMINADA en KRONOS. Ingresa a tu "
                        + "dashboard para ver el detalle.", "/index");

        for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
            notificacionService.crear(gestor, "🎓 " + aprendiz.getNombre() + " " + aprendiz.getApellido()
                    + " (ficha " + aprendizFicha.getFicha().getNumeroFicha()
                    + ") completó sus bitácoras y el Formato 023 — su Etapa Productiva quedó TERMINADA.", "/gestor/aprendices");
        }
    }

    // ─────────────────────────────────── Helpers ───────────────────────────────────

    private EtapaProductiva etapaDelInstructor(Long idUsuarioInstructor, Long idEtapa) {
        EtapaProductiva etapa = etapaProductivaRepository.findById(idEtapa)
                .orElseThrow(() -> new IllegalArgumentException("La etapa productiva indicada no existe."));
        validarEtapaAsignada(idUsuarioInstructor, etapa);
        return etapa;
    }

    // Candado de autorización: la etapa debe tener asignación vigente a este Instructor de Seguimiento
    private void validarEtapaAsignada(Long idUsuarioInstructor, EtapaProductiva etapa) {
        boolean asignada = etapasAsignadas(idUsuarioInstructor).stream()
                .anyMatch(e -> e.getIdEtapa().equals(etapa.getIdEtapa()));
        if (!asignada) {
            throw new IllegalArgumentException("Ese aprendiz no está asignado a tu seguimiento.");
        }
    }

    // Candado de autorización: la etapa debe ser del aprendiz logueado
    private EtapaProductiva etapaDelAprendiz(Long idUsuarioAprendiz, Long idEtapa) {
        EtapaProductiva etapa = etapaProductivaRepository.findById(idEtapa)
                .orElseThrow(() -> new IllegalArgumentException("La etapa productiva indicada no existe."));
        if (!etapa.getAprendizFicha().getUsuario().getIdUsuario().equals(idUsuarioAprendiz)) {
            throw new IllegalArgumentException("Esa etapa productiva no te pertenece.");
        }
        return etapa;
    }

    private String limpiarObservacion(ResultadoEvaluacion resultado, String observaciones) {
        if (resultado != ResultadoEvaluacion.APROBADO && (observaciones == null || observaciones.isBlank())) {
            throw new IllegalArgumentException("Debes escribir la novedad indicando qué debe corregir o por qué se reprueba.");
        }
        return (observaciones != null && !observaciones.isBlank()) ? observaciones.trim() : null;
    }

    private String mensajeEvaluacion(String etiqueta, ResultadoEvaluacion resultado, String observacion, boolean femenino) {
        String aprobado = femenino ? "aprobada" : "aprobado";
        String reprobado = femenino ? "reprobada" : "reprobado";
        return switch (resultado) {
            case APROBADO -> "✅ Tu " + etiqueta + " fue " + aprobado + ".";
            case REPROBADO -> "❌ Tu " + etiqueta + " fue " + reprobado + ". Novedad: " + observacion;
            case CORREGIR -> "✏️ Tu " + etiqueta + " requiere corrección. Novedad: " + observacion;
        };
    }

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? SIN_DATO : texto;
    }

    private String fecha(LocalDate f) {
        return f != null ? f.format(FORMATO_FECHA) : SIN_DATO;
    }

    private String vacioANull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private boolean noVacio(String texto) {
        return texto != null && !texto.isBlank();
    }

    // Mismo patrón de guardado que SeguimientoEtapaService.guardarArchivo, para evidencias del Momento 1
    private String guardarArchivoGenerico(MultipartFile archivo, String subcarpeta, Long idEtapa) {
        try {
            Path directorio = Paths.get(uploadRootDir, subcarpeta, "etapa_" + idEtapa);
            Files.createDirectories(directorio);
            String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
            int puntoIdx = nombreOriginal.lastIndexOf('.');
            String extension = puntoIdx >= 0 ? nombreOriginal.substring(puntoIdx).toLowerCase() : "";
            String nombreArchivo = java.util.UUID.randomUUID() + extension;
            Path destino = directorio.resolve(nombreArchivo);
            archivo.transferTo(destino);
            return "/" + destino.toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo en el servidor: " + e.getMessage(), e);
        }
    }

    // Mismo patrón que PerfilController.guardarFotoPerfil: valida por Content-Type image/*
    private String guardarImagen(MultipartFile imagen, String subcarpeta, Long idEtapa) {
        String tipoContenido = imagen.getContentType();
        if (tipoContenido == null || !tipoContenido.startsWith("image/")) {
            throw new IllegalArgumentException("La firma debe ser una imagen (JPG, PNG, etc).");
        }
        return guardarArchivoGenerico(imagen, subcarpeta, idEtapa);
    }
}
