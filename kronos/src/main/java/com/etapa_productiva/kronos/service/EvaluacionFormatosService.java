package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.AprendizBitacoraDetalleDto;
import com.etapa_productiva.kronos.dto.AprendizBitacoraResumenDto;
import com.etapa_productiva.kronos.dto.AprendizPlaneacionResumenDto;
import com.etapa_productiva.kronos.dto.BitacoraEvaluarDto;
import com.etapa_productiva.kronos.dto.CertificacionInfoDto;
import com.etapa_productiva.kronos.dto.DashboardBitacorasDto;
import com.etapa_productiva.kronos.dto.DashboardInstructorDto.BarraGrafico;
import com.etapa_productiva.kronos.dto.DashboardInstructorDto.SegmentoGrafico;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.Bitacora;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EstadoAcademico;
import com.etapa_productiva.kronos.entity.EstadoEtapa;
import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.EvaluacionBitacora;
import com.etapa_productiva.kronos.entity.EvaluacionPlaneacion;
import com.etapa_productiva.kronos.entity.FormatoPlaneacion;
import com.etapa_productiva.kronos.entity.InstructorSeguimiento;
import com.etapa_productiva.kronos.entity.ResultadoEvaluacion;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.BitacoraRepository;
import com.etapa_productiva.kronos.repository.CronogramaBitacorasRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.EvaluacionBitacoraRepository;
import com.etapa_productiva.kronos.repository.EvaluacionPlaneacionRepository;
import com.etapa_productiva.kronos.repository.FormatoPlaneacionRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.repository.VisitaSeguimientoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * Corregir), deja la novedad si aplica y notifica al aprendiz.
 */
@Service
public class EvaluacionFormatosService {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIN_DATO = "—";

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
    private EvaluacionPlaneacionRepository evaluacionPlaneacionRepository;

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
        notificacionService.crear(aprendiz, mensajeEvaluacion("Bitácora N°" + numero, resultado, observacionLimpia, true));

        verificarYTransicionarPorCertificar(etapa);
    }

    // ─────────────────────────── Formato de Planeación (023) ───────────────────────────

    /** Un renglón por aprendiz asignado al instructor, con su Formato de Planeación. */
    @Transactional(readOnly = true)
    public List<AprendizPlaneacionResumenDto> listarResumenPlaneacion(Long idUsuarioInstructor) {
        List<AprendizPlaneacionResumenDto> filas = new ArrayList<>();

        for (EtapaProductiva etapa : etapasAsignadas(idUsuarioInstructor)) {
            Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
            FormatoPlaneacion formato = formatoPlaneacionRepository.findByEtapaProductivaIdEtapa(etapa.getIdEtapa()).orElse(null);

            String estado = "Sin radicar";
            String observaciones = null;
            if (formato != null) {
                EvaluacionPlaneacion ev = evaluacionPlaneacionRepository
                        .findTopByFormatoPlaneacionIdFormatoPlaneacionOrderByFechaEvaluacionDesc(formato.getIdFormatoPlaneacion())
                        .orElse(null);
                if (ev == null) {
                    estado = "En revisión";
                } else {
                    observaciones = ev.getObservaciones();
                    estado = switch (ev.getResultado()) {
                        case APROBADO -> "Aprobado";
                        case REPROBADO -> "Reprobado";
                        case CORREGIR -> "Para corregir";
                    };
                }
            }

            filas.add(AprendizPlaneacionResumenDto.builder()
                    .idEtapa(etapa.getIdEtapa())
                    .idFormatoPlaneacion(formato != null ? formato.getIdFormatoPlaneacion() : null)
                    .nombres(valor(aprendiz.getNombre()))
                    .apellidos(valor(aprendiz.getApellido()))
                    .documento(valor(aprendiz.getDocumento()))
                    .ficha(etapa.getAprendizFicha().getFicha().getNumeroFicha())
                    .asunto(formato != null ? formato.getAsunto() : SIN_DATO)
                    .fechaSubida(formato != null ? formato.getFechaHoraSubida().format(FORMATO_FECHA_HORA) : SIN_DATO)
                    .rutaArchivo(formato != null ? formato.getRutaArchivo() : null)
                    .estado(estado)
                    .observaciones(observaciones)
                    .build());
        }

        filas.sort(Comparator.comparing(AprendizPlaneacionResumenDto::getApellidos, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AprendizPlaneacionResumenDto::getNombres, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    /** ✅ Registra la evaluación del Formato de Planeación (023) y notifica al aprendiz. */
    @Transactional
    public void evaluarPlaneacion(Long idUsuarioInstructor, Long idFormatoPlaneacion, ResultadoEvaluacion resultado, String observaciones) {
        FormatoPlaneacion formato = formatoPlaneacionRepository.findById(idFormatoPlaneacion)
                .orElseThrow(() -> new IllegalArgumentException("El Formato de Planeación indicado no existe."));
        EtapaProductiva etapa = formato.getEtapaProductiva();
        validarEtapaAsignada(idUsuarioInstructor, etapa);

        String observacionLimpia = limpiarObservacion(resultado, observaciones);

        Usuario instructor = usuarioRepository.findById(idUsuarioInstructor)
                .orElseThrow(() -> new IllegalStateException("No se encontró tu usuario."));

        evaluacionPlaneacionRepository.save(EvaluacionPlaneacion.builder()
                .formatoPlaneacion(formato)
                .instructor(instructor)
                .resultado(resultado)
                .observaciones(observacionLimpia)
                .build());

        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
        notificacionService.crear(aprendiz, mensajeEvaluacion("Formato de Planeación (023)", resultado, observacionLimpia, false));

        verificarYTransicionarPorCertificar(etapa);
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

        boolean formatoAprobado = false;
        FormatoPlaneacion formato = formatoPlaneacionRepository.findByEtapaProductivaIdEtapa(etapa.getIdEtapa()).orElse(null);
        if (formato != null) {
            EvaluacionPlaneacion ev = evaluacionPlaneacionRepository
                    .findTopByFormatoPlaneacionIdFormatoPlaneacionOrderByFechaEvaluacionDesc(formato.getIdFormatoPlaneacion())
                    .orElse(null);
            formatoAprobado = ev != null && ev.getResultado() == ResultadoEvaluacion.APROBADO;
        }

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
        etapa.setFechaPorCertificar(java.time.LocalDateTime.now());
        etapaProductivaRepository.save(etapa);

        AprendizFicha aprendizFicha = etapa.getAprendizFicha();
        aprendizFicha.setEstadoAcademico(EstadoAcademico.CERTIFICADO);
        aprendizFichaRepository.save(aprendizFicha);

        Usuario aprendiz = aprendizFicha.getUsuario();
        notificacionService.crear(aprendiz,
                "🎉 ¡Completaste el 100% de tu Etapa Productiva! Tu Instructor de Seguimiento aprobó tus bitácoras "
                        + "y el Formato 023, así que tu Etapa Productiva ya quedó TERMINADA en KRONOS. Ingresa a tu "
                        + "dashboard para ver el detalle.");

        for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
            notificacionService.crear(gestor, "🎓 " + aprendiz.getNombre() + " " + aprendiz.getApellido()
                    + " (ficha " + aprendizFicha.getFicha().getNumeroFicha()
                    + ") completó sus bitácoras y el Formato 023 — su Etapa Productiva quedó TERMINADA.");
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
}
