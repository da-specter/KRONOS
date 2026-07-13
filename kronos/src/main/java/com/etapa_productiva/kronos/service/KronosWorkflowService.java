package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.EtapaProductiva; // Import exacto de tu entidad
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.entity.EstadoEtapa;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import com.etapa_productiva.kronos.entity.AccionRealizada;
import com.etapa_productiva.kronos.entity.AprendizFicha;             // Entidades de tus relaciones
import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.Departamento;
import com.etapa_productiva.kronos.entity.DocumentoSolicitud;
import com.etapa_productiva.kronos.entity.Empresa;
import com.etapa_productiva.kronos.entity.EstadoFiltro;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.HistorialNovedad;
import com.etapa_productiva.kronos.entity.InstructorSeguimiento;
import com.etapa_productiva.kronos.entity.Municipio;
import com.etapa_productiva.kronos.entity.Novedad;
import com.etapa_productiva.kronos.entity.PlantillaFormato;
import com.etapa_productiva.kronos.entity.SeccionFormato;
import com.etapa_productiva.kronos.entity.TipoContrato;
import com.etapa_productiva.kronos.entity.TipoNovedad;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.DepartamentoRepository;
import com.etapa_productiva.kronos.repository.DocumentoSolicitudRepository;
import com.etapa_productiva.kronos.repository.EmpresaRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;
import com.etapa_productiva.kronos.repository.MunicipioRepository;
import com.etapa_productiva.kronos.repository.HistorialNovedadRepository;
import com.etapa_productiva.kronos.repository.NovedadRepository;
import com.etapa_productiva.kronos.repository.PlantillaFormatoRepository;
import com.etapa_productiva.kronos.repository.SeccionFormatoRepository;
import com.etapa_productiva.kronos.repository.TipoContratoRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
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
import java.util.List;
import java.util.Optional;
import com.etapa_productiva.kronos.entity.ModalidadEtapa;

@Service
public class KronosWorkflowService {

    private static final List<String> EXTENSIONES_PERMITIDAS = List.of(".doc", ".docx", ".xls", ".xlsx");

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private TipoContratoRepository tipoContratoRepository;

    @Autowired
    private SeccionFormatoRepository seccionFormatoRepository;

    @Autowired
    private PlantillaFormatoRepository plantillaFormatoRepository;

    @Autowired
    private DocumentoSolicitudRepository documentoSolicitudRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private MunicipioRepository municipioRepository;

    @Autowired
    private InstructorSeguimientoRepository instructorSeguimientoRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private NovedadRepository novedadRepository;

    @Autowired
    private HistorialNovedadRepository historialNovedadRepository;

    @Autowired
    private CronogramaService cronogramaService;

    @Value("${app.upload.dir:uploads/documentos-solicitud}")
    private String uploadDir;

    @Value("${app.upload.mensajes-registro-dir:uploads/mensajes-registro}")
    private String mensajesRegistroDir;

    private static final List<String> EXTENSIONES_PERMITIDAS_CHAT = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx");

    /**
     * 👨‍🎓 PASO 1: El Aprendiz inicia el proceso creando su solicitud en el sistema.
     * Ahora utiliza de forma segura el Enum para la modalidad que aspira.
     * Si el aprendiz ya tenía una solicitud RECHAZADA, se reutiliza esa misma fila
     * (reenvío): se resetean los checks y el motivo de rechazo y vuelve a PENDIENTE_REVISION.
     */
    @Transactional
    public SolicitudEtapaPractica aprendizCrearSolicitud(Long idAprendizFicha, Long idSeccionFormato, ModalidadEtapa modalidad) {
        AprendizFicha aprendizFicha = aprendizFichaRepository.findById(idAprendizFicha)
                .orElseThrow(() -> new RuntimeException("Aprendiz-Ficha no encontrado con ID: " + idAprendizFicha));
        SeccionFormato seccionFormato = seccionFormatoRepository.findById(idSeccionFormato)
                .orElseThrow(() -> new RuntimeException("Modalidad de contrato no encontrada con ID: " + idSeccionFormato));

        // 📅 Defensa de servidor (espejo del gate de /index): sin importar la duración de la
        // ficha, la Etapa Práctica solo habilita 6 meses antes de que termine. Evita que se
        // radique una solicitud saltándose el formulario con un POST directo.
        // 💼 Excepción: Vinculación Laboral habilita 3 meses antes que las demás (9 meses antes
        // del fin de la ficha), por el tiempo que toma formalizar un contrato laboral.
        Ficha ficha = aprendizFicha.getFicha();
        boolean habilitada = seccionFormato.esVinculacionLaboral()
                ? ficha.isVinculacionLaboralHabilitada()
                : ficha.isEtapaPracticaHabilitada();
        if (!habilitada) {
            LocalDate fechaHabilitacion = seccionFormato.esVinculacionLaboral()
                    ? ficha.getFechaHabilitacionVinculacionLaboral()
                    : ficha.getFechaHabilitacionEtapaPractica();
            throw new IllegalStateException("Tu ficha aún está en Etapa Lectiva: la modalidad "
                    + seccionFormato.getNombreSeccion() + " se habilita el "
                    + fechaHabilitacion.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
        }

        Optional<SolicitudEtapaPractica> solicitudExistente = solicitudRepository.findByAprendizFichaIdAprendizFicha(idAprendizFicha);

        SolicitudEtapaPractica solicitud;
        if (solicitudExistente.isPresent()) {
            solicitud = solicitudExistente.get();

            // Además del reenvío tras un RECHAZADO, también se permite radicar una nueva
            // solicitud si la Etapa Productiva de la anterior ya quedó TERMINADA (el aprendiz
            // terminó su ciclo anterior y quiere iniciar uno nuevo).
            boolean rechazada = solicitud.getEstado() == EstadoSolicitud.RECHAZADO;
            boolean cicloCertificado = solicitud.getEstado() == EstadoSolicitud.APROBADO_EN_ETAPA
                    && etapaProductivaRepository.findByAprendizIdUsuario(aprendizFicha.getUsuario().getIdUsuario())
                            .map(e -> e.getEstadoEtapa() == EstadoEtapa.TERMINADO)
                            .orElse(false);

            if (!rechazada && !cicloCertificado) {
                throw new IllegalStateException("El aprendiz ya cuenta con una solicitud activa en el sistema.");
            }
            // Reenvío o nuevo ciclo tras certificación: se reutiliza la misma fila para no
            // romper la relación 1-a-1 aprendiz/solicitud
            solicitud.setCheckFechaEstipulada(false);
            solicitud.setCheckCompetenciasAprobadas(false);
            solicitud.setCheckModalidadAprobada(false);
            solicitud.setCheckFormatosRadicados(false);
            solicitud.setRutaFormatosSubidos(null);
            solicitud.setPlantillasHabilitadas(false);
            solicitud.setObservacionRechazo(null);
        } else {
            solicitud = new SolicitudEtapaPractica();
            solicitud.setAprendizFicha(aprendizFicha);
        }

        // 🎓 Contrato de Aprendizaje ya se gestiona en Sofía Plus: salta directo a Registro,
        // sin pasar por los checks de fecha/competencias/documentos del Gestor de Etapa.
        boolean esContratoAprendizaje = seccionFormato.esContratoAprendizaje();

        solicitud.setSeccionFormato(seccionFormato);
        solicitud.setModalidadSolicitada(modalidad); // 🚀 Ya no fallará por tipo
        solicitud.setEstado(esContratoAprendizaje
                ? EstadoSolicitud.PENDIENTE_REGISTRO
                : EstadoSolicitud.PENDIENTE_REVISION);
        solicitud.setFechaActualizacion(LocalDateTime.now());

        SolicitudEtapaPractica guardada = solicitudRepository.save(solicitud);

        Usuario aprendizRemitente = aprendizFicha.getUsuario();
        if (esContratoAprendizaje) {
            for (Usuario registro : usuarioRepository.findAllRegistroActivos()) {
                notificacionService.crear(registro, "🎓 " + aprendizRemitente.getNombre() + " " + aprendizRemitente.getApellido()
                        + " radicó una solicitud de Contrato de Aprendizaje: ya puedes evaluarla en tu Bandeja de Solicitudes.");
            }
        } else {
            for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
                notificacionService.crear(gestor, "📝 " + aprendizRemitente.getNombre() + " " + aprendizRemitente.getApellido()
                        + " radicó una nueva solicitud de etapa productiva (" + modalidad + ").");
            }
        }

        return guardada;
    }

    /**
     * 👨‍💼 PASO 2 y 3: El Coordinador evalúa el primer filtro (Bandeja de entrada).
     * Digita los checks de Fecha Estipulada y Competencias Aprobadas en la interfaz.
     * Si rechaza algún check, deja una novedad (observación) que se notifica al aprendiz.
     *
     * 💼 Excepción exclusiva de Vinculación Laboral: el check de Competencias Aprobadas no la
     * bloquea. Si el Gestor de Etapa lo deja sin marcar mientras la fecha sí cumple, la
     * solicitud avanza igual a FORMATOS_HABILITADOS (el aprendiz puede ver sus formatos).
     */
    @Transactional
    public SolicitudEtapaPractica coordinadorEvaluarPrimerFiltro(Long idSolicitud, boolean fechaOk, boolean competenciasOk, String observacion) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE_REVISION) {
            throw new IllegalStateException("Acción denegada: La solicitud no se encuentra pendiente de revisión inicial.");
        }

        solicitud.setCheckFechaEstipulada(fechaOk);
        solicitud.setCheckCompetenciasAprobadas(competenciasOk);

        boolean competenciasAplican = !solicitud.getSeccionFormato().esVinculacionLaboral();

        // Si cumple fecha (y competencias, salvo en Vinculación Laboral), se habilita el módulo de formatos
        if (fechaOk && (competenciasOk || !competenciasAplican)) {
            solicitud.setEstado(EstadoSolicitud.FORMATOS_HABILITADOS);
            solicitud.setObservacionRechazo(null);
        } else {
            if (observacion == null || observacion.isBlank()) {
                throw new IllegalArgumentException("Debes escribir una novedad indicando el motivo del rechazo.");
            }
            solicitud.setEstado(EstadoSolicitud.RECHAZADO);
            solicitud.setObservacionRechazo(observacion);
        }

        solicitud.setFechaActualizacion(LocalDateTime.now());
        SolicitudEtapaPractica actualizada = solicitudRepository.save(solicitud);

        String mensaje;
        if (actualizada.getEstado() == EstadoSolicitud.FORMATOS_HABILITADOS) {
            mensaje = "¡Tu solicitud fue aprobada en el primer filtro! Ya puedes subir tus formatos de "
                    + actualizada.getSeccionFormato().getNombreSeccion() + " diligenciados.";
        } else {
            mensaje = "❌ Tu solicitud fue rechazada por el Gestor de Etapa. Novedad: " + observacion;
        }
        notificacionService.crear(actualizada.getAprendizFicha().getUsuario(), mensaje);

        return actualizada;
    }

    private static final int MAXIMO_ARCHIVOS_REQUISITOS = 10;

    /**
     * 👨‍🎓 PASO 4: El Aprendiz descarga los documentos del formato de su modalidad, los
     * diligencia y sube hasta {@value #MAXIMO_ARCHIVOS_REQUISITOS} archivos radicados al
     * sistema, cada uno con un asunto obligatorio que describe su contenido. Se guardan como
     * DocumentoSolicitud (sin plantilla asociada, por ser formatos libres) para que el
     * Gestor de Etapa pueda ver los asuntos en su bandeja.
     */
    @Transactional
    public SolicitudEtapaPractica aprendizSubirFormatos(Long idSolicitud, List<MultipartFile> archivos, List<String> asuntos) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.FORMATOS_HABILITADOS) {
            throw new IllegalStateException("Acción denegada: La solicitud no se encuentra en estado de carga de formatos.");
        }

        if (archivos == null || asuntos == null || archivos.size() != asuntos.size()) {
            throw new IllegalArgumentException("Cada archivo debe traer su asunto correspondiente.");
        }

        // Se descartan las filas del formulario que quedaron vacías (el aprendiz no llenó todas las que agregó)
        List<MultipartFile> archivosUsados = new java.util.ArrayList<>();
        List<String> asuntosUsados = new java.util.ArrayList<>();
        for (int i = 0; i < archivos.size(); i++) {
            MultipartFile archivo = archivos.get(i);
            if (archivo != null && !archivo.isEmpty()) {
                archivosUsados.add(archivo);
                asuntosUsados.add(asuntos.get(i));
            }
        }

        if (archivosUsados.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos un archivo para subir.");
        }
        if (archivosUsados.size() > MAXIMO_ARCHIVOS_REQUISITOS) {
            throw new IllegalArgumentException("Puedes subir máximo " + MAXIMO_ARCHIVOS_REQUISITOS + " archivos.");
        }

        try {
            Path directorio = Paths.get(uploadDir, "solicitud_" + idSolicitud);
            Files.createDirectories(directorio);

            for (int i = 0; i < archivosUsados.size(); i++) {
                MultipartFile archivo = archivosUsados.get(i);
                String asunto = asuntosUsados.get(i) == null ? "" : asuntosUsados.get(i).trim();
                if (asunto.isBlank()) {
                    throw new IllegalArgumentException("Todos los archivos deben indicar un asunto que describa su contenido.");
                }

                String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
                int puntoIdx = nombreOriginal.lastIndexOf('.');
                String extension = puntoIdx >= 0 ? nombreOriginal.substring(puntoIdx).toLowerCase() : "";
                if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
                    throw new IllegalArgumentException("Solo se aceptan archivos de Word (.doc, .docx) o Excel (.xls, .xlsx).");
                }

                String nombreArchivo = java.util.UUID.randomUUID() + extension;
                Path destino = directorio.resolve(nombreArchivo);
                archivo.transferTo(destino);

                DocumentoSolicitud documento = DocumentoSolicitud.builder()
                        .solicitud(solicitud)
                        .plantillaFormato(null)
                        .rutaArchivoLleno(rutaWeb(destino))
                        .asunto(asunto)
                        .estadoValidacion(EstadoValidacion.PENDIENTE)
                        .fechaSubida(LocalDateTime.now())
                        .build();
                documentoSolicitudRepository.save(documento);
            }

            solicitud.setEstado(EstadoSolicitud.FORMATOS_ENVIADOS); // Viaja a la bandeja del Gestor de Etapa
            // El panel de descarga/resubida de plantillas se habilita de inmediato: el Gestor de
            // Etapa ya no tiene un paso manual aparte, solo califica al final (gestorCalificarDocumentos).
            solicitud.setPlantillasHabilitadas(true);
            solicitud.setFechaActualizacion(LocalDateTime.now());

            SolicitudEtapaPractica guardada = solicitudRepository.save(solicitud);

            notificacionService.crear(guardada.getAprendizFicha().getUsuario(),
                    "📄 Recibimos tus formatos. Ya puedes descargar, diligenciar y firmar tus plantillas para subirlas.");

            Usuario aprendizFormatos = guardada.getAprendizFicha().getUsuario();
            for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
                notificacionService.crear(gestor, "📨 " + aprendizFormatos.getNombre() + " " + aprendizFormatos.getApellido()
                        + " (ficha " + guardada.getAprendizFicha().getFicha().getNumeroFicha()
                        + ") envió sus documentos requisitos diligenciados para tu revisión.");
            }

            return guardada;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo en el servidor: " + e.getMessage(), e);
        }
    }

    /**
     * 👨‍🎓 El Aprendiz descarga la plantilla en blanco, la diligencia/firma y la resube aquí
     * (multipart/form-data). Solo disponible una vez el Gestor de Etapa habilitó el panel.
     */
    @Transactional
    public DocumentoSolicitud aprendizSubirPlantillaFirmada(Long idSolicitud, Long idPlantilla, MultipartFile archivo) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (!solicitud.isPlantillasHabilitadas()) {
            throw new IllegalStateException("El panel de plantillas aún no ha sido habilitado por el Gestor de Etapa.");
        }

        PlantillaFormato plantilla = plantillaFormatoRepository.findById(idPlantilla)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con ID: " + idPlantilla));

        if (plantilla.getSeccionFormato() == null
                || !plantilla.getSeccionFormato().getIdSeccionFormato().equals(solicitud.getSeccionFormato().getIdSeccionFormato())) {
            throw new IllegalArgumentException("La plantilla indicada no corresponde a la modalidad de contrato de tu solicitud.");
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar un archivo para subir.");
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
        int puntoIdx = nombreOriginal.lastIndexOf('.');
        String extension = puntoIdx >= 0 ? nombreOriginal.substring(puntoIdx).toLowerCase() : "";
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Solo se aceptan archivos de Word (.doc, .docx) o Excel (.xls, .xlsx).");
        }

        try {
            Path directorio = Paths.get(uploadDir, "solicitud_" + idSolicitud);
            Files.createDirectories(directorio);
            String nombreArchivo = "plantilla_" + idPlantilla + "_" + System.currentTimeMillis() + extension;
            Path destino = directorio.resolve(nombreArchivo);
            archivo.transferTo(destino);

            DocumentoSolicitud documento = documentoSolicitudRepository
                    .findBySolicitudIdSolicitudAndPlantillaFormatoIdPlantilla(idSolicitud, idPlantilla)
                    .orElseGet(DocumentoSolicitud::new);
            documento.setSolicitud(solicitud);
            documento.setPlantillaFormato(plantilla);
            documento.setRutaArchivoLleno(rutaWeb(destino));
            documento.setEstadoValidacion(EstadoValidacion.PENDIENTE);
            documento.setFechaSubida(LocalDateTime.now());

            DocumentoSolicitud guardado = documentoSolicitudRepository.save(documento);

            Usuario aprendizPlantilla = solicitud.getAprendizFicha().getUsuario();
            for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
                notificacionService.crear(gestor, "📨 " + aprendizPlantilla.getNombre() + " " + aprendizPlantilla.getApellido()
                        + " (ficha " + solicitud.getAprendizFicha().getFicha().getNumeroFicha()
                        + ") subió la plantilla firmada \"" + plantilla.getNombreDocumento() + "\".");
            }

            return guardado;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo en el servidor: " + e.getMessage(), e);
        }
    }

    /**
     * 👨‍💼 Único punto de control del Gestor de Etapa sobre los documentos/plantillas firmadas
     * que subió el aprendiz: aprueba o rechaza modalidad y formatos. Si aprueba ambos, la
     * solicitud viaja a la bandeja del rol REGISTRO para su propia validación (ya NO crea la
     * Etapa Productiva aquí). Si rechaza, vuelve a la bandeja del aprendiz (FORMATOS_HABILITADOS)
     * para que corrija y reenvíe sus documentos, sin tener que radicar una solicitud nueva.
     */
    @Transactional
    public SolicitudEtapaPractica gestorCalificarDocumentos(Long idSolicitud, boolean modalidadOk, boolean formatosOk, String observacion) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.FORMATOS_ENVIADOS) {
            throw new IllegalStateException("Acción denegada: La solicitud no se encuentra en estado de calificación de documentos.");
        }

        solicitud.setCheckModalidadAprobada(modalidadOk);
        solicitud.setCheckFormatosRadicados(formatosOk);

        if (!modalidadOk || !formatosOk) {
            if (observacion == null || observacion.isBlank()) {
                throw new IllegalArgumentException("Debes escribir una novedad indicando el motivo del rechazo.");
            }
            solicitud.setEstado(EstadoSolicitud.FORMATOS_HABILITADOS);
            solicitud.setObservacionRechazo(observacion);
            solicitud.setFechaActualizacion(LocalDateTime.now());
            SolicitudEtapaPractica devuelta = solicitudRepository.save(solicitud);

            notificacionService.crear(solicitud.getAprendizFicha().getUsuario(),
                    "❌ El Gestor de Etapa rechazó tus documentos. Corrige y vuelve a enviarlos. Novedad: " + observacion);

            return devuelta;
        }

        solicitud.setObservacionRechazo(null);
        solicitud.setEstado(EstadoSolicitud.EN_VALIDACION_REGISTRO);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        SolicitudEtapaPractica actualizada = solicitudRepository.save(solicitud);

        Usuario aprendiz = actualizada.getAprendizFicha().getUsuario();
        for (Usuario registro : usuarioRepository.findAllRegistroActivos()) {
            notificacionService.crear(registro, "📄 El Gestor de Etapa calificó los documentos de "
                    + aprendiz.getNombre() + " " + aprendiz.getApellido() + ": ya están listos para tu validación.");
        }

        notificacionService.crear(aprendiz,
                "✅ El Gestor de Etapa aprobó tu modalidad y tus formatos. Tus documentos pasaron a validación del área de Registro.");

        return actualizada;
    }

    /**
     * 👨‍💼 PASO 6: El rol REGISTRO valida los documentos que el Gestor de Etapa ya calificó.
     * Si aprueba, la solicitud queda lista para registrar la Etapa Productiva. Si rechaza,
     * vuelve a la bandeja del Gestor de Etapa (FORMATOS_ENVIADOS) con la novedad de Registro,
     * para que decida si corrige y reenvía o rechaza definitivamente al aprendiz.
     */
    @Transactional
    public SolicitudEtapaPractica registroValidarDocumentos(Long idSolicitud, boolean aprobado, String observacion) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.EN_VALIDACION_REGISTRO) {
            throw new IllegalStateException("Acción denegada: La solicitud no se encuentra en validación de Registro.");
        }

        Usuario aprendiz = solicitud.getAprendizFicha().getUsuario();

        if (aprobado) {
            solicitud.setObservacionRechazo(null);
            solicitud.setEstado(EstadoSolicitud.LISTO_PARA_REGISTRO);
            solicitud.setFechaActualizacion(LocalDateTime.now());
            SolicitudEtapaPractica actualizada = solicitudRepository.save(solicitud);

            // 📋 Este es el punto en que KRONOS considera "cerrado" el expediente de documentos:
            // marca APROBADO cada DocumentoSolicitud de la solicitud (formatos diligenciados y
            // plantillas firmadas). Antes de esto el campo nacía en PENDIENTE y nunca se tocaba,
            // lo que inflaba para siempre el widget "Documentos Pendientes de Validación" del
            // Gestor y dejaba vacío el Reporte Aprendiz (que solo busca documentos APROBADO).
            List<DocumentoSolicitud> documentos = documentoSolicitudRepository.findBySolicitudIdSolicitud(idSolicitud);
            documentos.forEach(documento -> documento.setEstadoValidacion(EstadoValidacion.APROBADO));
            documentoSolicitudRepository.saveAll(documentos);

            notificacionService.crear(aprendiz,
                    "✅ Registro validó tus documentos. Tu Etapa Productiva será registrada en breve.");

            for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
                notificacionService.crear(gestor, "✅ Registro validó los documentos de "
                        + aprendiz.getNombre() + " " + aprendiz.getApellido()
                        + ": la solicitud quedó lista para registrar la Etapa Productiva.");
            }

            return actualizada;
        }

        if (observacion == null || observacion.isBlank()) {
            throw new IllegalArgumentException("Debes escribir una novedad indicando el motivo del rechazo.");
        }

        solicitud.setCheckModalidadAprobada(false);
        solicitud.setCheckFormatosRadicados(false);
        solicitud.setEstado(EstadoSolicitud.FORMATOS_ENVIADOS);
        solicitud.setObservacionRechazo(observacion);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        SolicitudEtapaPractica devuelta = solicitudRepository.save(solicitud);

        for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
            notificacionService.crear(gestor, "↩️ Registro devolvió la solicitud de "
                    + aprendiz.getNombre() + " " + aprendiz.getApellido() + ". Novedad: " + observacion);
        }

        return devuelta;
    }

    /**
     * 🎓 Bandeja exclusiva de Contrato de Aprendizaje (rol REGISTRO): a diferencia de
     * {@link #registroValidarDocumentos}, aquí no hay checklist de documentos — la etapa
     * productiva ya se gestiona en Sofía Plus, así que Registro solo aprueba o no con una
     * novedad opcional (obligatoria si rechaza). Si aprueba, la solicitud cae directo en la
     * bandeja de "Registro Etapa Productiva" (LISTO_PARA_REGISTRO).
     */
    @Transactional
    public SolicitudEtapaPractica registroEvaluarSolicitudContratoAprendizaje(Long idSolicitud, boolean aprobado, String novedad) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE_REGISTRO) {
            throw new IllegalStateException("Acción denegada: La solicitud no se encuentra pendiente de aprobación de Registro.");
        }

        Usuario aprendiz = solicitud.getAprendizFicha().getUsuario();

        if (aprobado) {
            solicitud.setObservacionRechazo(null);
            solicitud.setEstado(EstadoSolicitud.LISTO_PARA_REGISTRO);
            solicitud.setFechaActualizacion(LocalDateTime.now());
            SolicitudEtapaPractica actualizada = solicitudRepository.save(solicitud);

            String mensaje = "✅ Registro aprobó tu solicitud de Contrato de Aprendizaje. Tu Etapa Productiva será registrada en breve.";
            if (novedad != null && !novedad.isBlank()) {
                mensaje += " Novedad: " + novedad;
            }
            notificacionService.crear(aprendiz, mensaje);

            return actualizada;
        }

        if (novedad == null || novedad.isBlank()) {
            throw new IllegalArgumentException("Debes escribir una novedad indicando el motivo del rechazo.");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADO);
        solicitud.setObservacionRechazo(novedad);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        SolicitudEtapaPractica devuelta = solicitudRepository.save(solicitud);

        notificacionService.crear(aprendiz,
                "❌ Registro rechazó tu solicitud de Contrato de Aprendizaje. Novedad: " + novedad);

        return devuelta;
    }

    /**
     * 🏢 Módulo "Registro Etapa Productiva" (rol REGISTRO): inyecta la información en la
     * entidad real usando el patrón Builder, dejando trazabilidad de qué usuario la registró.
     */
    @Transactional
    public EtapaProductiva crearEtapaProductivaDesdeSolicitud(
            Long idSolicitud,
            Long idAprendizFicha,              // ID de la relación ManyToOne (se resuelve contra la BD, no se confía en el body)
            Long idEmpresa,                    // ID de la relación ManyToOne
            Long idTipoContrato,                // ID de la relación ManyToOne
            LocalDate fechaInicio,             // Columna FECHA_INICIO (Imagen 2)
            LocalDate fechaFin,                // Columna FECHA_FIN (Imagen 2)
            String nombreJefeInmediato,        // Columna NOMBRE_JEFE_INMEDIATO (Imagen 3)
            String correoJefeInmediato,        // Columna CORREO_JEFE_INMEDIATO (Imagen 3)
            String telefonoJefeInmediato,       // Columna TELEFONO_JEFE_INMEDIATO (Imagen 3)
            Long idUsuarioRegistro) {           // Usuario del rol REGISTRO que realiza el registro

        // 0. Vigencia reglamentaria: fin posterior al inicio y máximo 6 meses de duración
        com.etapa_productiva.kronos.util.ValidacionCampos.validarRangoEtapa(fechaInicio, fechaFin);

        // 1. Validar y cerrar la solicitud intermedia del flujo de trabajo
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.LISTO_PARA_REGISTRO) {
            throw new IllegalStateException("Acción denegada: La solicitud aún no fue validada por Registro.");
        }

        if (!solicitud.getAprendizFicha().getIdAprendizFicha().equals(idAprendizFicha)) {
            throw new IllegalArgumentException("El aprendiz indicado no coincide con el que radicó la solicitud.");
        }

        // 2. Resolver las entidades reales en la BD a partir de los IDs recibidos (nunca confiar en objetos armados por el cliente)
        AprendizFicha aprendizFicha = aprendizFichaRepository.findById(idAprendizFicha)
                .orElseThrow(() -> new RuntimeException("Aprendiz-Ficha no encontrado con ID: " + idAprendizFicha));
        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con ID: " + idEmpresa));
        TipoContrato tipoContrato = tipoContratoRepository.findById(idTipoContrato)
                .orElseThrow(() -> new RuntimeException("Tipo de contrato no encontrado con ID: " + idTipoContrato));
        Usuario usuarioRegistro = usuarioRepository.findById(idUsuarioRegistro)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuarioRegistro));

        solicitud.setEstado(EstadoSolicitud.APROBADO_EN_ETAPA);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        // 3. 🚀 CREACIÓN DE TU ENTIDAD REAL CON EL PATRÓN BUILDER DE LOMBOK
        EtapaProductiva nuevaEtapa = EtapaProductiva.builder()
                .aprendizFicha(aprendizFicha)
                .empresa(empresa)
                .tipoContrato(tipoContrato)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .modalidad(solicitud.getModalidadSolicitada()) // Asigna el Enum de modalidad guardado en la solicitud
                .nombreJefeInmediato(nombreJefeInmediato)
                .correoJefeInmediato(correoJefeInmediato)
                .telefonoJefeInmediato(telefonoJefeInmediato)
                .usuarioRegistro(usuarioRegistro)
                .build();                                      // estadoEtapa se auto-inicializa gracias a tu @PrePersist

        // Guardar definitivamente en Oracle mediante tu repositorio oficial
        EtapaProductiva etapaGuardada = etapaProductivaRepository.save(nuevaEtapa);

        // 4. 📅 Automatización del calendario: genera las 12 bitácoras quincenales reglamentarias
        cronogramaService.generarCronograma(etapaGuardada);

        Usuario aprendizDestino = aprendizFicha.getUsuario();
        notificacionService.crear(aprendizDestino,
                "🎉 ¡Tu Etapa Productiva ya fue registrada y está activa! Ya puedes subir tus bitácoras y tu formato de planeación.");

        for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
            notificacionService.crear(gestor, "🏢 Registro registró la Etapa Productiva de "
                    + aprendizDestino.getNombre() + " " + aprendizDestino.getApellido()
                    + " en " + empresa.getNombreEmpresa() + " (del " + fechaInicio + " al " + fechaFin + ").");
        }

        return etapaGuardada;
    }

    /**
     * 🏢 Módulo "Registro Etapa Productiva": el rol REGISTRO digita libremente los datos
     * de la empresa, el municipio/departamento y el tipo de contrato (sin catálogos previos).
     * Resuelve o crea cada catálogo por su clave natural y delega en el registro ya probado.
     */
    @Transactional
    public EtapaProductiva registrarEtapaProductiva(
            Long idSolicitud,
            Long idAprendizFicha,
            String nit,
            String nombreEmpresa,
            String direccionEmpresa,
            String telefonoEmpresa,
            String correoEmpresa,
            String nombreMunicipio,
            String nombreDepartamento,
            String nombreTipoContrato,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String nombreJefeInmediato,
            String correoJefeInmediato,
            String telefonoJefeInmediato,
            Long idUsuarioRegistro) {

        Departamento departamento = departamentoRepository.findByNombreDepartamentoIgnoreCase(nombreDepartamento)
                .orElseGet(() -> departamentoRepository.save(
                        Departamento.builder().nombreDepartamento(nombreDepartamento).build()));

        Municipio municipio = municipioRepository
                .findByNombreMunicipioIgnoreCaseAndDepartamentoIdDepartamento(nombreMunicipio, departamento.getIdDepartamento())
                .orElseGet(() -> municipioRepository.save(
                        Municipio.builder().nombreMunicipio(nombreMunicipio).departamento(departamento).build()));

        Empresa empresa = empresaRepository.findByNit(nit)
                .orElseGet(() -> empresaRepository.save(
                        Empresa.builder()
                                .nit(nit)
                                .nombreEmpresa(nombreEmpresa)
                                .direccion(direccionEmpresa)
                                .telefono(telefonoEmpresa)
                                .correo(correoEmpresa)
                                .municipio(municipio)
                                .build()));

        TipoContrato tipoContrato = tipoContratoRepository.findByNombreTipoContratoIgnoreCase(nombreTipoContrato)
                .orElseGet(() -> tipoContratoRepository.save(
                        TipoContrato.builder().nombreTipoContrato(nombreTipoContrato).build()));

        return crearEtapaProductivaDesdeSolicitud(
                idSolicitud, idAprendizFicha,
                empresa.getIdEmpresa(), tipoContrato.getIdTipoContrato(),
                fechaInicio, fechaFin, nombreJefeInmediato, correoJefeInmediato, telefonoJefeInmediato, idUsuarioRegistro);
    }

    /**
     * ✏️ El rol REGISTRO corrige los datos de una Etapa Productiva ya registrada (empresa,
     * contrato, fechas, jefe inmediato y estado). Resuelve o crea cada catálogo por su clave
     * natural, igual que al registrar, y guarda los cambios directamente en la entidad real.
     */
    @Transactional
    public EtapaProductiva editarEtapaProductiva(
            Long idEtapa,
            String nit,
            String nombreEmpresa,
            String direccionEmpresa,
            String telefonoEmpresa,
            String correoEmpresa,
            String nombreMunicipio,
            String nombreDepartamento,
            String nombreTipoContrato,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String nombreJefeInmediato,
            String correoJefeInmediato,
            String telefonoJefeInmediato,
            EstadoEtapa estadoEtapa) {

        EtapaProductiva etapa = etapaProductivaRepository.findById(idEtapa)
                .orElseThrow(() -> new RuntimeException("Etapa Productiva no encontrada con ID: " + idEtapa));

        com.etapa_productiva.kronos.util.ValidacionCampos.validarRangoEtapa(fechaInicio, fechaFin);

        Departamento departamento = departamentoRepository.findByNombreDepartamentoIgnoreCase(nombreDepartamento)
                .orElseGet(() -> departamentoRepository.save(
                        Departamento.builder().nombreDepartamento(nombreDepartamento).build()));

        Municipio municipio = municipioRepository
                .findByNombreMunicipioIgnoreCaseAndDepartamentoIdDepartamento(nombreMunicipio, departamento.getIdDepartamento())
                .orElseGet(() -> municipioRepository.save(
                        Municipio.builder().nombreMunicipio(nombreMunicipio).departamento(departamento).build()));

        Empresa empresa = empresaRepository.findByNit(nit)
                .orElseGet(() -> empresaRepository.save(
                        Empresa.builder()
                                .nit(nit)
                                .nombreEmpresa(nombreEmpresa)
                                .direccion(direccionEmpresa)
                                .telefono(telefonoEmpresa)
                                .correo(correoEmpresa)
                                .municipio(municipio)
                                .build()));

        TipoContrato tipoContrato = tipoContratoRepository.findByNombreTipoContratoIgnoreCase(nombreTipoContrato)
                .orElseGet(() -> tipoContratoRepository.save(
                        TipoContrato.builder().nombreTipoContrato(nombreTipoContrato).build()));

        etapa.setEmpresa(empresa);
        etapa.setTipoContrato(tipoContrato);
        etapa.setFechaInicio(fechaInicio);
        etapa.setFechaFin(fechaFin);
        etapa.setNombreJefeInmediato(nombreJefeInmediato);
        etapa.setCorreoJefeInmediato(correoJefeInmediato);
        etapa.setTelefonoJefeInmediato(telefonoJefeInmediato);
        etapa.setEstadoEtapa(estadoEtapa);

        EtapaProductiva editada = etapaProductivaRepository.save(etapa);

        Usuario aprendizEditado = editada.getAprendizFicha().getUsuario();
        for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
            notificacionService.crear(gestor, "✏️ Registro actualizó la Etapa Productiva de "
                    + aprendizEditado.getNombre() + " " + aprendizEditado.getApellido()
                    + " (empresa " + empresa.getNombreEmpresa() + ", estado " + estadoEtapa + ").");
        }

        return editada;
    }

    /**
     * 👥 El Gestor de Etapa asigna un Instructor de Seguimiento a un aprendiz puntual
     * (su Etapa Productiva ya activa), sin depender de la asignación por Ficha completa.
     * Queda registrado en ASIGNACION_INSTRUCTOR_ETAPA (trazabilidad histórica), no como
     * un FK único sobrescribible en ETAPA_PRODUCTIVA.
     */
    @Transactional
    public AsignacionInstructorEtapa asignarInstructorSeguimiento(Long idEtapa, Long idInstructorSeguimiento) {
        EtapaProductiva etapa = etapaProductivaRepository.findById(idEtapa)
                .orElseThrow(() -> new RuntimeException("Etapa Productiva no encontrada con ID: " + idEtapa));

        if (asignacionInstructorEtapaRepository.findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(idEtapa).isPresent()) {
            throw new IllegalStateException("Este aprendiz ya tiene un Instructor de Seguimiento asignado.");
        }

        InstructorSeguimiento instructor = instructorSeguimientoRepository.findById(idInstructorSeguimiento)
                .orElseThrow(() -> new RuntimeException("Instructor de Seguimiento no encontrado con ID: " + idInstructorSeguimiento));

        AsignacionInstructorEtapa asignacion = AsignacionInstructorEtapa.builder()
                .etapaProductiva(etapa)
                .instructor(instructor)
                .estadoAsignacion(true)
                .build();

        AsignacionInstructorEtapa guardada = asignacionInstructorEtapaRepository.save(asignacion);

        Usuario aprendizDestino = etapa.getAprendizFicha().getUsuario();
        Usuario instructorDestino = instructor.getUsuario();

        notificacionService.crear(aprendizDestino,
                "👨‍🏫 Se te asignó a " + instructorDestino.getNombre() + " " + instructorDestino.getApellido()
                        + " como tu Instructor de Seguimiento.");

        notificacionService.crear(instructorDestino,
                "👨‍🎓 Se te asignó el seguimiento de " + aprendizDestino.getNombre() + " " + aprendizDestino.getApellido() + ".");

        return guardada;
    }

    /**
     * 📢 El Aprendiz radica una Novedad (suspensión, aplazamiento, cambio de empresa, reclamo, etc.)
     * sobre su propia Etapa Productiva activa, eligiendo si va dirigida a su Instructor de
     * Seguimiento asignado o al Gestor de Etapa. Notifica al destinatario de inmediato.
     */
    @Transactional
    public Novedad reportarNovedad(Long idUsuarioAprendiz, Long idEtapa, TipoNovedad tipoNovedad, String descripcion, String destinatarioTipo, MultipartFile archivo) {
        EtapaProductiva etapa = etapaProductivaRepository.findById(idEtapa)
                .orElseThrow(() -> new RuntimeException("Etapa Productiva no encontrada con ID: " + idEtapa));

        if (!etapa.getAprendizFicha().getUsuario().getIdUsuario().equals(idUsuarioAprendiz)) {
            throw new IllegalArgumentException("Esta Etapa Productiva no te pertenece.");
        }

        Usuario remitente = usuarioRepository.findById(idUsuarioAprendiz)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuarioAprendiz));

        Usuario destinatario;
        if ("INSTRUCTOR".equals(destinatarioTipo)) {
            AsignacionInstructorEtapa asignacion = asignacionInstructorEtapaRepository
                    .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(idEtapa)
                    .orElseThrow(() -> new IllegalStateException("Aún no tienes un Instructor de Seguimiento asignado."));
            destinatario = asignacion.getInstructor().getUsuario();
        } else {
            destinatario = usuarioRepository.findPrimerGestorEtapaActivo()
                    .orElseThrow(() -> new IllegalStateException("No hay ningún Gestor de Etapa activo disponible para recibir la novedad."));
        }

        Novedad novedad = Novedad.builder()
                .etapaProductiva(etapa)
                .remitente(remitente)
                .destinatarioAc(destinatario)
                .tipoNovedad(tipoNovedad)
                .descripcion(descripcion)
                .urlSoporte(guardarAdjuntoNovedad(archivo))
                .build();

        Novedad guardada = novedadRepository.save(novedad);

        notificacionService.crear(destinatario,
                "📢 Nueva novedad de " + remitente.getNombre() + " " + remitente.getApellido()
                        + " (" + tipoNovedad + "): " + descripcion);

        return guardada;
    }

    /**
     * 💬 El Instructor de Seguimiento inicia una conversación en Novedades con uno de sus
     * aprendices asignados (asignación vigente): queda guardada como Novedad tipo OTRO sobre
     * la Etapa Productiva del aprendiz, con adjunto opcional, y se le notifica de inmediato.
     */
    @Transactional
    public Novedad instructorEnviarNovedadAprendiz(Long idUsuarioInstructor, Long idEtapa, String mensaje, MultipartFile archivo) {
        Usuario remitente = usuarioRepository.findById(idUsuarioInstructor)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuarioInstructor));

        AsignacionInstructorEtapa asignacion = asignacionInstructorEtapaRepository
                .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(idEtapa)
                .orElseThrow(() -> new IllegalStateException("Esa Etapa Productiva no tiene un Instructor de Seguimiento asignado."));

        if (!asignacion.getInstructor().getUsuario().getIdUsuario().equals(idUsuarioInstructor)) {
            throw new IllegalArgumentException("Solo puedes enviar mensajes a los aprendices que tienes asignados.");
        }

        if ((mensaje == null || mensaje.isBlank()) && (archivo == null || archivo.isEmpty())) {
            throw new IllegalArgumentException("Debes escribir un mensaje o adjuntar un archivo.");
        }

        EtapaProductiva etapa = asignacion.getEtapaProductiva();
        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();

        Novedad novedad = Novedad.builder()
                .etapaProductiva(etapa)
                .remitente(remitente)
                .destinatarioAc(aprendiz)
                .tipoNovedad(TipoNovedad.OTRO)
                .descripcion(mensaje == null ? "" : mensaje)
                .urlSoporte(guardarAdjuntoNovedad(archivo))
                .build();
        Novedad guardada = novedadRepository.save(novedad);

        notificacionService.crear(aprendiz,
                "💬 Tu Instructor de Seguimiento " + remitente.getNombre() + " " + remitente.getApellido()
                        + " te escribió en Novedades: " + (mensaje == null || mensaje.isBlank() ? "(adjunto)" : mensaje));

        return guardada;
    }

    /**
     * 💬 El Instructor de Seguimiento o el Gestor de Etapa responde una Novedad dirigida a él,
     * dejando trazabilidad en HISTORIAL_NOVEDAD y notificando la respuesta de vuelta al aprendiz.
     * Excepción: las novedades INFORMATIVO (chat GESTOR_ETAPA ↔ REGISTRO) no tienen un
     * destinatario fijo — puede responder cualquier usuario activo de esos dos roles.
     */
    @Transactional
    public HistorialNovedad responderNovedad(Long idNovedad, Long idUsuarioAccion, String comentarioRespuesta, AccionRealizada accionRealizada) {
        Novedad novedad = novedadRepository.findById(idNovedad)
                .orElseThrow(() -> new RuntimeException("Novedad no encontrada con ID: " + idNovedad));

        Usuario usuarioAccion = usuarioRepository.findById(idUsuarioAccion)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuarioAccion));

        if (novedad.getTipoNovedad() == TipoNovedad.INFORMATIVO || novedad.getTipoNovedad() == TipoNovedad.COORD_ACADEMICO) {
            List<String> rolesAccion = usuarioAccion.getUsuarioRoles().stream()
                    .map(ur -> ur.getRol().getNombreRol())
                    .toList();
            String rolContraparte = novedad.getTipoNovedad() == TipoNovedad.INFORMATIVO ? "REGISTRO" : "COORDINADOR_ACADEMICO";
            if (!rolesAccion.contains("GESTOR_ETAPA") && !rolesAccion.contains(rolContraparte)) {
                throw new IllegalArgumentException("No tienes permisos para responder en este canal.");
            }
        } else if (!novedad.getDestinatarioAc().getIdUsuario().equals(idUsuarioAccion)) {
            throw new IllegalArgumentException("Esta novedad no está dirigida a ti.");
        }

        HistorialNovedad historial = HistorialNovedad.builder()
                .novedad(novedad)
                .usuarioAccion(usuarioAccion)
                .comentarioRespuesta(comentarioRespuesta)
                .accionRealizada(accionRealizada)
                .build();
        HistorialNovedad guardado = historialNovedadRepository.save(historial);

        if (accionRealizada == AccionRealizada.APROBAR) {
            novedad.setEstadoFiltro(EstadoFiltro.RESUELTO);
        } else if (accionRealizada == AccionRealizada.RECHAZAR) {
            novedad.setEstadoFiltro(EstadoFiltro.RECHAZADO);
        } else if (accionRealizada == AccionRealizada.ESCALAR) {
            novedad.setEstadoFiltro(EstadoFiltro.ESCALADO_COORDINACION);
        } else {
            novedad.setEstadoFiltro(EstadoFiltro.EN_REVISION_INS);
        }
        novedadRepository.save(novedad);

        if (novedad.getTipoNovedad() == TipoNovedad.INFORMATIVO) {
            String texto = "💬 " + usuarioAccion.getNombre() + " " + usuarioAccion.getApellido()
                    + " respondió en Novedades: " + comentarioRespuesta;
            for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
                if (!gestor.getIdUsuario().equals(idUsuarioAccion)) {
                    notificacionService.crear(gestor, texto);
                }
            }
            for (Usuario registro : usuarioRepository.findAllRegistroActivos()) {
                if (!registro.getIdUsuario().equals(idUsuarioAccion)) {
                    notificacionService.crear(registro, texto);
                }
            }
        } else if (novedad.getTipoNovedad() == TipoNovedad.COORD_ACADEMICO) {
            String texto = "💬 " + usuarioAccion.getNombre() + " " + usuarioAccion.getApellido()
                    + " respondió en Novedades: " + comentarioRespuesta;
            for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
                if (!gestor.getIdUsuario().equals(idUsuarioAccion)) {
                    notificacionService.crear(gestor, texto);
                }
            }
            for (Usuario coordinador : usuarioRepository.findAllCoordinacionAcademicaActivos()) {
                if (!coordinador.getIdUsuario().equals(idUsuarioAccion)) {
                    notificacionService.crear(coordinador, texto);
                }
            }
        } else {
            notificacionService.crear(novedad.getRemitente(),
                    "💬 Respuesta a tu novedad (" + novedad.getTipoNovedad() + "): " + comentarioRespuesta);
        }

        return guardado;
    }

    /**
     * 💬 Chat de Novedades GESTOR_ETAPA ↔ REGISTRO: un mensaje informativo (con adjunto
     * opcional) que cualquier usuario de uno de estos dos roles puede radicar, guardado como
     * Novedad tipo INFORMATIVO (sin Etapa Productiva asociada). Visible para todos los usuarios
     * de ambos roles a través del mismo módulo "Novedades" que ya usa el Gestor de Etapa.
     */
    @Transactional
    public Novedad enviarNovedadInformativa(Long idUsuarioRemitente, String descripcion, MultipartFile archivo) {
        Usuario remitente = usuarioRepository.findById(idUsuarioRemitente)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuarioRemitente));

        List<String> rolesRemitente = remitente.getUsuarioRoles().stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();
        boolean esGestor = rolesRemitente.contains("GESTOR_ETAPA");
        boolean esRegistro = rolesRemitente.contains("REGISTRO");
        if (!esGestor && !esRegistro) {
            throw new IllegalArgumentException("Solo el Gestor de Etapa o el rol Registro pueden enviar mensajes en este canal.");
        }

        if ((descripcion == null || descripcion.isBlank()) && (archivo == null || archivo.isEmpty())) {
            throw new IllegalArgumentException("Debes escribir un mensaje o adjuntar un archivo.");
        }

        Usuario destinatario = esGestor
                ? usuarioRepository.findPrimerRegistroActivo()
                        .orElseThrow(() -> new IllegalStateException("No hay ningún usuario del rol Registro activo disponible."))
                : usuarioRepository.findPrimerGestorEtapaActivo()
                        .orElseThrow(() -> new IllegalStateException("No hay ningún Gestor de Etapa activo disponible."));

        Novedad novedad = Novedad.builder()
                .etapaProductiva(null)
                .remitente(remitente)
                .destinatarioAc(destinatario)
                .tipoNovedad(TipoNovedad.INFORMATIVO)
                .descripcion(descripcion == null ? "" : descripcion)
                .urlSoporte(guardarAdjuntoNovedad(archivo))
                .build();
        Novedad guardada = novedadRepository.save(novedad);

        String textoNotificacion = "💬 Nuevo mensaje de " + remitente.getNombre() + " " + remitente.getApellido() + " en Novedades.";
        for (Usuario u : esGestor ? usuarioRepository.findAllRegistroActivos() : usuarioRepository.findAllGestoresEtapaActivos()) {
            if (!u.getIdUsuario().equals(idUsuarioRemitente)) {
                notificacionService.crear(u, textoNotificacion);
            }
        }

        return guardada;
    }

    /**
     * 💬 Chat de Novedades GESTOR_ETAPA ↔ COORDINADOR_ACADEMICO: mismo mecanismo que el chat con
     * Registro (Novedad tipo COORD_ACADEMICO, sin Etapa Productiva asociada, canal separado del
     * de Registro para que ningún rol vea los mensajes del otro).
     */
    @Transactional
    public Novedad enviarNovedadCoordinacion(Long idUsuarioRemitente, String descripcion, MultipartFile archivo) {
        Usuario remitente = usuarioRepository.findById(idUsuarioRemitente)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuarioRemitente));

        List<String> rolesRemitente = remitente.getUsuarioRoles().stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();
        boolean esGestor = rolesRemitente.contains("GESTOR_ETAPA");
        boolean esCoordinador = rolesRemitente.contains("COORDINADOR_ACADEMICO");
        if (!esGestor && !esCoordinador) {
            throw new IllegalArgumentException("Solo el Gestor de Etapa o Coordinación Académica pueden enviar mensajes en este canal.");
        }

        if ((descripcion == null || descripcion.isBlank()) && (archivo == null || archivo.isEmpty())) {
            throw new IllegalArgumentException("Debes escribir un mensaje o adjuntar un archivo.");
        }

        Usuario destinatario = esGestor
                ? usuarioRepository.findPrimerCoordinacionAcademicaActivo()
                        .orElseThrow(() -> new IllegalStateException("No hay ningún usuario de Coordinación Académica activo disponible."))
                : usuarioRepository.findPrimerGestorEtapaActivo()
                        .orElseThrow(() -> new IllegalStateException("No hay ningún Gestor de Etapa activo disponible."));

        Novedad novedad = Novedad.builder()
                .etapaProductiva(null)
                .remitente(remitente)
                .destinatarioAc(destinatario)
                .tipoNovedad(TipoNovedad.COORD_ACADEMICO)
                .descripcion(descripcion == null ? "" : descripcion)
                .urlSoporte(guardarAdjuntoNovedad(archivo))
                .build();
        Novedad guardada = novedadRepository.save(novedad);

        String textoNotificacion = "💬 Nuevo mensaje de " + remitente.getNombre() + " " + remitente.getApellido() + " en Novedades.";
        for (Usuario u : esGestor ? usuarioRepository.findAllCoordinacionAcademicaActivos() : usuarioRepository.findAllGestoresEtapaActivos()) {
            if (!u.getIdUsuario().equals(idUsuarioRemitente)) {
                notificacionService.crear(u, textoNotificacion);
            }
        }

        return guardada;
    }

    // Convierte una ruta física local (con separadores de Windows) en una URL servible por WebConfig (/uploads/**)
    private String rutaWeb(Path destino) {
        return "/" + destino.toString().replace('\\', '/');
    }

    // 📎 Guarda el adjunto opcional de una Novedad (chat de staff o novedad radicada por el
    // aprendiz): valida la extensión contra la allowlist, lo guarda con nombre aleatorio y
    // devuelve la URL servible por WebConfig. Null si no se adjuntó ningún archivo.
    private String guardarAdjuntoNovedad(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
        int puntoIdx = nombreOriginal.lastIndexOf('.');
        String extension = puntoIdx >= 0 ? nombreOriginal.substring(puntoIdx).toLowerCase() : "";
        if (!EXTENSIONES_PERMITIDAS_CHAT.contains(extension)) {
            throw new IllegalArgumentException("Solo se aceptan imágenes, PDF, Word o Excel.");
        }

        try {
            Path directorio = Paths.get(mensajesRegistroDir);
            Files.createDirectories(directorio);
            String nombreArchivo = java.util.UUID.randomUUID() + extension;
            Path destino = directorio.resolve(nombreArchivo);
            archivo.transferTo(destino);
            return rutaWeb(destino);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo en el servidor: " + e.getMessage(), e);
        }
    }
}