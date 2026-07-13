package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.InstructorAprendizDto;
import com.etapa_productiva.kronos.dto.VisitaAgendaDto;
import com.etapa_productiva.kronos.dto.VisitasAgendaResumenDto;
import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.ModalidadVisita;
import com.etapa_productiva.kronos.entity.TipoVisita;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.repository.VisitaSeguimientoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 📅 Módulo "Agendar Visitas" del Instructor de Seguimiento: agenda visitas de seguimiento a
 * los aprendices que tiene asignados, permite cancelarlas o aplazarlas (con novedad) mientras
 * estén futuras o pendientes de hoy, notifica al aprendiz en cada paso y arma la agenda
 * (pasadas/pendientes/futuras) tanto para el instructor como para el cronograma del propio
 * aprendiz. Las visitas canceladas o aplazadas pasan automáticamente a la franja "Pasadas",
 * sin importar su fecha original.
 *
 * Cada Etapa Productiva se acompaña con 3 visitas: el instructor solo agenda la primera
 * (KRONOS lo alerta 15 días después del inicio si aún no lo ha hecho) y el sistema agenda
 * automáticamente las otras 2, bien distribuidas en el tiempo restante hasta el fin de la etapa.
 */
@Service
public class VisitaSeguimientoService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] TITULOS_EXPORT = {
            "Fecha", "Aprendiz", "Ficha", "Tipo", "Modalidad", "Estado", "Novedad"
    };

    private static final Set<String> EXTENSIONES_EVIDENCIA = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".jpg", ".jpeg", ".png");

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    @Autowired
    private VisitaSeguimientoRepository visitaSeguimientoRepository;

    @Autowired
    private InstructorSeguimientoRepository instructorSeguimientoRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstructorSeguimientoService instructorSeguimientoService;

    @Autowired
    private NotificacionService notificacionService;

    /** Aprendices asignados al instructor, para poblar el buscador del formulario de agendamiento. */
    @Transactional(readOnly = true)
    public List<InstructorAprendizDto> listarAprendicesParaAgendar(Long idUsuarioInstructor) {
        return instructorSeguimientoService.listarAprendices(idUsuarioInstructor);
    }

    /**
     * ⚠️ Aprendices asignados al instructor que hoy no tienen ninguna visita vigente
     * (PLANEADA y no vencida): quedan fuera aunque hayan tenido una visita en el pasado,
     * si esta ya se realizó, se canceló o se aplazó sin volver a agendarse.
     */
    @Transactional(readOnly = true)
    public List<InstructorAprendizDto> listarAprendicesSinVisitaVigente(Long idUsuarioInstructor) {
        List<InstructorAprendizDto> asignados = listarAprendicesParaAgendar(idUsuarioInstructor);
        Set<Long> etapasConVisitaVigente = visitaSeguimientoRepository
                .findByInstructorIdUsuarioOrderByFechaVisitaAsc(idUsuarioInstructor).stream()
                .filter(this::esGestionable)
                .map(v -> v.getEtapaProductiva().getIdEtapa())
                .collect(Collectors.toSet());
        return asignados.stream()
                .filter(a -> !etapasConVisitaVigente.contains(a.getIdEtapa()))
                .toList();
    }

    // La Etapa Productiva se acompaña con exactamente 3 visitas de seguimiento a lo largo de su vigencia.
    private static final int TOTAL_VISITAS_SEGUIMIENTO = 3;

    /**
     * Agenda una nueva visita de seguimiento y notifica de inmediato al aprendiz.
     * Valida que la Etapa Productiva elegida esté realmente asignada a este instructor
     * (evita que un instructor agende visitas a aprendices que no son suyos).
     * La novedad es opcional: información adicional que el instructor quiera dejar registrada.
     *
     * 📅 Si esta es la PRIMERA visita que se agenda para la Etapa Productiva, KRONOS agenda
     * automáticamente las otras 2 restantes, distribuidas en tercios sobre el tiempo que queda
     * entre esta fecha y el fin de la etapa (ver {@link #autoAgendarVisitasRestantes}).
     */
    @Transactional
    public VisitaSeguimiento agendarVisita(Long idUsuarioInstructor, Long idEtapa, LocalDateTime fecha,
                                            String modalidadEtiqueta, String novedad) {
        var instructor = instructorSeguimientoRepository.findByUsuarioIdUsuario(idUsuarioInstructor)
                .orElseThrow(() -> new IllegalStateException("Tu perfil de Instructor de Seguimiento no está configurado."));

        boolean asignado = asignacionInstructorEtapaRepository
                .findByInstructorIdInstructorSeguimientoAndEstadoAsignacionTrue(instructor.getIdInstructorSeguimiento())
                .stream()
                .map(AsignacionInstructorEtapa::getEtapaProductiva)
                .anyMatch(e -> e.getIdEtapa().equals(idEtapa));
        if (!asignado) {
            throw new IllegalArgumentException("Ese aprendiz no está asignado a tu seguimiento.");
        }

        if (fecha == null) {
            throw new IllegalArgumentException("Debes elegir una fecha para la visita.");
        }

        EtapaProductiva etapa = etapaProductivaRepository.findById(idEtapa)
                .orElseThrow(() -> new IllegalArgumentException("La Etapa Productiva indicada no existe."));

        Usuario usuarioInstructor = usuarioRepository.findById(idUsuarioInstructor)
                .orElseThrow(() -> new IllegalStateException("Usuario del instructor no encontrado."));

        boolean esLaPrimeraVisita = !visitaSeguimientoRepository.existsByEtapaProductivaIdEtapa(idEtapa);
        ModalidadVisita modalidad = mapearModalidad(modalidadEtiqueta);

        VisitaSeguimiento visita = visitaSeguimientoRepository.save(VisitaSeguimiento.builder()
                .etapaProductiva(etapa)
                .instructor(usuarioInstructor)
                .fechaVisita(fecha)
                .tipoVisita(TipoVisita.SEGUIMIENTO)
                .modalidad(modalidad)
                .estadoVisita(EstadoVisita.PLANEADA)
                .observaciones(recortarNovedad(novedad))
                .build());

        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
        String mensaje = "📅 Tu Instructor de Seguimiento agendó tu visita de seguimiento para el " + fecha.format(FORMATO_FECHA) + ".";
        if (novedad != null && !novedad.isBlank()) {
            mensaje += " Novedad: " + novedad.trim();
        }
        notificacionService.crear(aprendiz, mensaje);

        if (esLaPrimeraVisita) {
            autoAgendarVisitasRestantes(etapa, usuarioInstructor, aprendiz, fecha, modalidad);
        }

        return visita;
    }

    /**
     * 🤖 Agenda automáticamente las 2 visitas restantes (de las 3 reglamentarias) apenas se
     * registra la primera, distribuyéndolas en tercios sobre el tiempo que queda hasta el fin
     * de la Etapa Productiva: la 2ª al primer tercio del tiempo restante y la 3ª al segundo
     * tercio, dejando siempre un margen antes de la fecha de fin. Si queda muy poco tiempo
     * (menos de 4 días) no se auto-agendan: el instructor deberá programarlas manualmente.
     */
    private void autoAgendarVisitasRestantes(EtapaProductiva etapa, Usuario usuarioInstructor, Usuario aprendiz,
                                              LocalDateTime fechaPrimeraVisita, ModalidadVisita modalidad) {
        LocalDate fechaFin = etapa.getFechaFin();
        long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(fechaPrimeraVisita.toLocalDate(), fechaFin);

        if (diasRestantes < 4) {
            notificacionService.crear(usuarioInstructor,
                    "⚠️ Queda poco tiempo hasta el fin de la Etapa Productiva de " + aprendiz.getNombre() + " "
                            + aprendiz.getApellido() + ": agenda tú mismo las 2 visitas de seguimiento restantes.");
            return;
        }

        // Las 2 visitas auto-agendadas conservan la misma hora que eligió el instructor para la primera.
        long tercio = diasRestantes / 3;
        LocalDateTime fechaSegunda = fechaPrimeraVisita.plusDays(tercio);
        LocalDateTime fechaTercera = fechaPrimeraVisita.plusDays(tercio * 2);
        if (!fechaTercera.toLocalDate().isBefore(fechaFin)) {
            fechaTercera = fechaFin.minusDays(1).atTime(fechaPrimeraVisita.toLocalTime());
        }
        if (!fechaSegunda.toLocalDate().isBefore(fechaTercera.toLocalDate())) {
            fechaSegunda = fechaPrimeraVisita.plusDays(1);
        }

        visitaSeguimientoRepository.save(VisitaSeguimiento.builder()
                .etapaProductiva(etapa)
                .instructor(usuarioInstructor)
                .fechaVisita(fechaSegunda)
                .tipoVisita(TipoVisita.SEGUIMIENTO)
                .modalidad(modalidad)
                .estadoVisita(EstadoVisita.PLANEADA)
                .observaciones("Visita 2 de " + TOTAL_VISITAS_SEGUIMIENTO + " — agendada automáticamente por KRONOS.")
                .build());

        visitaSeguimientoRepository.save(VisitaSeguimiento.builder()
                .etapaProductiva(etapa)
                .instructor(usuarioInstructor)
                .fechaVisita(fechaTercera)
                .tipoVisita(TipoVisita.SEGUIMIENTO)
                .modalidad(modalidad)
                .estadoVisita(EstadoVisita.PLANEADA)
                .observaciones("Visita 3 de " + TOTAL_VISITAS_SEGUIMIENTO + " — agendada automáticamente por KRONOS.")
                .build());

        notificacionService.crear(aprendiz, "📅 KRONOS agendó automáticamente tus 2 visitas de seguimiento restantes: "
                + fechaSegunda.format(FORMATO_FECHA) + " y " + fechaTercera.format(FORMATO_FECHA) + ".");
        notificacionService.crear(usuarioInstructor, "📅 KRONOS agendó automáticamente las 2 visitas restantes de "
                + aprendiz.getNombre() + " " + aprendiz.getApellido() + ": "
                + fechaSegunda.format(FORMATO_FECHA) + " y " + fechaTercera.format(FORMATO_FECHA) + ".");
    }

    /**
     * Cancela o aplaza una visita ya agendada, dejando constancia de la novedad (motivo).
     * Solo permitido mientras la visita siga PLANEADA y su fecha no haya pasado (futura o
     * pendiente de hoy); una vez cancelada/aplazada, pasa automáticamente a "Pasadas".
     */
    @Transactional
    public VisitaSeguimiento cambiarEstadoVisita(Long idUsuarioInstructor, Long idVisita, String accion, String novedad) {
        VisitaSeguimiento visita = visitaSeguimientoRepository.findById(idVisita)
                .orElseThrow(() -> new IllegalArgumentException("La visita indicada no existe."));

        if (!visita.getInstructor().getIdUsuario().equals(idUsuarioInstructor)) {
            throw new IllegalArgumentException("Esta visita no fue agendada por ti.");
        }
        if (!esGestionable(visita)) {
            throw new IllegalStateException("Esta visita ya no se puede modificar (ya pasó o ya tiene un estado final).");
        }
        if (novedad == null || novedad.isBlank()) {
            throw new IllegalArgumentException("Debes escribir una novedad indicando el motivo.");
        }

        EstadoVisita nuevoEstado = switch (accion == null ? "" : accion.trim().toUpperCase(Locale.ROOT)) {
            case "CANCELAR" -> EstadoVisita.CANCELADA;
            case "APLAZAR" -> EstadoVisita.REPROGRAMADA;
            default -> throw new IllegalArgumentException("Acción inválida: debe ser cancelar o aplazar.");
        };

        visita.setEstadoVisita(nuevoEstado);
        visita.setObservaciones(recortarNovedad(novedad));
        VisitaSeguimiento guardada = visitaSeguimientoRepository.save(visita);

        Usuario aprendiz = visita.getEtapaProductiva().getAprendizFicha().getUsuario();
        String verbo = nuevoEstado == EstadoVisita.CANCELADA ? "cancelada" : "aplazada";
        String icono = nuevoEstado == EstadoVisita.CANCELADA ? "❌" : "🔁";
        notificacionService.crear(aprendiz, icono + " Tu visita de seguimiento " + etiquetaLegible(visita.getTipoVisita())
                + " del " + visita.getFechaVisita().format(FORMATO_FECHA) + " fue " + verbo + ". Novedad: " + novedad.trim());

        return guardada;
    }

    /** Agenda completa del instructor (todas las visitas que él mismo programó), en 3 franjas. */
    @Transactional
    public VisitasAgendaResumenDto listarAgendaInstructor(Long idUsuarioInstructor) {
        List<VisitaSeguimiento> visitas = visitaSeguimientoRepository.findByInstructorIdUsuarioOrderByFechaVisitaAsc(idUsuarioInstructor);
        marcarVencidasComoRealizadas(visitas);
        return agrupar(visitas);
    }

    /** Agenda de visitas de la Etapa Productiva de un aprendiz puntual (para su cronograma), en 3 franjas. */
    @Transactional
    public VisitasAgendaResumenDto listarAgendaAprendiz(Long idEtapa) {
        List<VisitaSeguimiento> visitas = visitaSeguimientoRepository.findByEtapaProductivaIdEtapaOrderByFechaVisitaAsc(idEtapa);
        marcarVencidasComoRealizadas(visitas);
        return agrupar(visitas);
    }

    /**
     * 🕓 Cualquier visita que siga PLANEADA una vez pasada su fecha (sin haberse cancelado
     * ni aplazado) se marca automáticamente como REALIZADA. Se ejecuta cada vez que alguien
     * consulta la agenda, así que no depende de un job en segundo plano para estar al día.
     */
    private void marcarVencidasComoRealizadas(List<VisitaSeguimiento> visitas) {
        LocalDate hoy = LocalDate.now();
        for (VisitaSeguimiento visita : visitas) {
            if (visita.getEstadoVisita() == EstadoVisita.PLANEADA && visita.getFechaVisita().toLocalDate().isBefore(hoy)) {
                visita.setEstadoVisita(EstadoVisita.REALIZADA);
                visitaSeguimientoRepository.save(visita);
            }
        }
    }

    /**
     * 📎 Adjunta (o reemplaza) el archivo de evidencia de una visita ya REALIZADA.
     * Solo el instructor que la agendó puede subirla, y solo cuando ya quedó realizada.
     */
    @Transactional
    public void subirEvidencia(Long idUsuarioInstructor, Long idVisita, MultipartFile archivo) {
        VisitaSeguimiento visita = visitaSeguimientoRepository.findById(idVisita)
                .orElseThrow(() -> new IllegalArgumentException("La visita indicada no existe."));

        if (!visita.getInstructor().getIdUsuario().equals(idUsuarioInstructor)) {
            throw new IllegalArgumentException("Esta visita no fue agendada por ti.");
        }
        if (visita.getEstadoVisita() != EstadoVisita.REALIZADA) {
            throw new IllegalStateException("Solo puedes adjuntar evidencia de visitas que ya quedaron realizadas.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar el archivo de evidencia.");
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
        int puntoIdx = nombreOriginal.lastIndexOf('.');
        String extension = puntoIdx >= 0 ? nombreOriginal.substring(puntoIdx).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSIONES_EVIDENCIA.contains(extension)) {
            throw new IllegalArgumentException("La evidencia debe ser un Word, Excel, PDF o imagen (JPG/PNG).");
        }

        try {
            Path directorio = Paths.get(uploadRootDir, "visitas", "visita_" + idVisita);
            Files.createDirectories(directorio);
            Path destino = directorio.resolve("evidencia_" + System.currentTimeMillis() + extension);
            archivo.transferTo(destino);
            visita.setRutaEvidencia("/" + destino.toString().replace('\\', '/'));
            visitaSeguimientoRepository.save(visita);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo de evidencia en el servidor: " + e.getMessage(), e);
        }
    }

    /** Exporta las visitas "pasadas" del instructor (histórico: vencidas, canceladas o aplazadas). */
    public byte[] exportarPasadasExcel(Long idUsuarioInstructor) throws IOException {
        List<VisitaAgendaDto> pasadas = listarAgendaInstructor(idUsuarioInstructor).getPasadas();
        return ExportacionUtil.excel("Visitas Pasadas", TITULOS_EXPORT, filasExport(pasadas));
    }

    public byte[] exportarPasadasPdf(Long idUsuarioInstructor) {
        List<VisitaAgendaDto> pasadas = listarAgendaInstructor(idUsuarioInstructor).getPasadas();
        return ExportacionUtil.pdf("KRONOS - Visitas de Seguimiento Pasadas", TITULOS_EXPORT, filasExport(pasadas));
    }

    private List<String[]> filasExport(List<VisitaAgendaDto> visitas) {
        List<String[]> filas = new ArrayList<>();
        for (VisitaAgendaDto v : visitas) {
            filas.add(new String[]{
                    v.getFecha(), v.getAprendizNombre(), v.getFicha(), v.getTipoEtiqueta(),
                    v.getModalidadEtiqueta(), v.getEstadoEtiqueta(),
                    v.getNovedad() != null ? v.getNovedad() : "—"
            });
        }
        return filas;
    }

    // Una visita solo se puede cancelar/aplazar mientras siga PLANEADA y su fecha no haya pasado
    private boolean esGestionable(VisitaSeguimiento visita) {
        return visita.getEstadoVisita() == EstadoVisita.PLANEADA
                && !visita.getFechaVisita().toLocalDate().isBefore(LocalDate.now());
    }

    private VisitasAgendaResumenDto agrupar(List<VisitaSeguimiento> visitas) {
        LocalDate hoy = LocalDate.now();
        List<VisitaAgendaDto> pasadas = new ArrayList<>();
        List<VisitaAgendaDto> pendientes = new ArrayList<>();
        List<VisitaAgendaDto> futuras = new ArrayList<>();

        for (VisitaSeguimiento visita : visitas) {
            VisitaAgendaDto dto = convertir(visita);
            // Canceladas, aplazadas o ya realizadas se consideran historial, sin importar su fecha
            boolean esHistorica = visita.getEstadoVisita() != EstadoVisita.PLANEADA;
            if (esHistorica || visita.getFechaVisita().toLocalDate().isBefore(hoy)) {
                pasadas.add(dto);
            } else if (visita.getFechaVisita().toLocalDate().isEqual(hoy)) {
                pendientes.add(dto);
            } else {
                futuras.add(dto);
            }
        }

        return VisitasAgendaResumenDto.builder().pasadas(pasadas).pendientes(pendientes).futuras(futuras).build();
    }

    private VisitaAgendaDto convertir(VisitaSeguimiento visita) {
        Usuario aprendiz = visita.getEtapaProductiva().getAprendizFicha().getUsuario();
        return VisitaAgendaDto.builder()
                .idVisita(visita.getIdVisita())
                .fecha(visita.getFechaVisita().format(FORMATO_FECHA))
                .aprendizNombre(aprendiz.getNombre() + " " + aprendiz.getApellido())
                .ficha(visita.getEtapaProductiva().getAprendizFicha().getFicha().getNumeroFicha())
                .tipoEtiqueta(etiquetaLegible(visita.getTipoVisita()))
                .modalidadEtiqueta(etiquetaLegible(visita.getModalidad()))
                .estado(visita.getEstadoVisita().name())
                .estadoEtiqueta(etiquetaLegible(visita.getEstadoVisita()))
                .novedad(visita.getObservaciones())
                .puedeGestionar(esGestionable(visita))
                .evidenciaRuta(visita.getRutaEvidencia())
                .build();
    }

    // Único tipo de visita existente: "Seguimiento". El método se mantiene para no romper
    // los lugares que ya piden una etiqueta legible por TipoVisita (agenda, exportaciones).
    private String etiquetaLegible(TipoVisita tipo) {
        return "Seguimiento";
    }

    // "Presencial"/"Virtual" (lo que elige el instructor) ↔ ModalidadVisita
    private ModalidadVisita mapearModalidad(String etiqueta) {
        if (etiqueta == null) {
            throw new IllegalArgumentException("Debes elegir la modalidad de la visita.");
        }
        return switch (etiqueta.trim().toUpperCase(Locale.ROOT)) {
            case "PRESENCIAL" -> ModalidadVisita.PRESENCIAL;
            case "VIRTUAL" -> ModalidadVisita.VIRTUAL;
            default -> throw new IllegalArgumentException("Modalidad de visita inválida.");
        };
    }

    private String etiquetaLegible(ModalidadVisita modalidad) {
        return switch (modalidad) {
            case PRESENCIAL -> "Presencial";
            case VIRTUAL -> "Virtual";
            case CANCELADA -> "Cancelada";
        };
    }

    /**
     * Único tipo de visita existente: siempre "Seguimiento", sin importar lo que traiga la
     * columna "tipo de visita" del Excel institucional (se conserva solo por compatibilidad
     * con plantillas de importación antiguas que aún incluyen esa columna).
     */
    public TipoVisita mapearTipoFlexible(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El tipo de visita está vacío.");
        }
        return TipoVisita.SEGUIMIENTO;
    }

    /**
     * Mapeo tolerante de texto libre (Excel institucional) a ModalidadVisita: acepta variantes
     * como "REUNIÓN VIRTUAL" en vez de exigir exactamente "PRESENCIAL"/"VIRTUAL".
     */
    public ModalidadVisita mapearModalidadFlexible(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("La modalidad de la visita está vacía.");
        }
        String t = texto.trim().toUpperCase(Locale.ROOT);
        if (t.contains("VIRTUAL")) return ModalidadVisita.VIRTUAL;
        if (t.contains("PRESENCIAL")) return ModalidadVisita.PRESENCIAL;
        throw new IllegalArgumentException("Modalidad de visita no reconocida: \"" + texto + "\".");
    }

    private String etiquetaLegible(EstadoVisita estado) {
        return switch (estado) {
            case PLANEADA -> "Planeada";
            case REALIZADA -> "Realizada";
            case REPROGRAMADA -> "Aplazada";
            case CANCELADA -> "Cancelada";
        };
    }

    // OBSERVACIONES es VARCHAR2(500) en Oracle
    private String recortarNovedad(String novedad) {
        if (novedad == null) return null;
        String limpio = novedad.trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() > 500 ? limpio.substring(0, 500) : limpio;
    }
}
