package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.EtapaProductiva; // Import exacto de tu entidad
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import com.etapa_productiva.kronos.entity.AccionAuditoria;
import com.etapa_productiva.kronos.entity.AccionRealizada;
import com.etapa_productiva.kronos.entity.AprendizFicha;             // Entidades de tus relaciones
import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.Auditoria;
import com.etapa_productiva.kronos.entity.Departamento;
import com.etapa_productiva.kronos.entity.DocumentoSolicitud;
import com.etapa_productiva.kronos.entity.Empresa;
import com.etapa_productiva.kronos.entity.EstadoFiltro;
import com.etapa_productiva.kronos.entity.HistorialNovedad;
import com.etapa_productiva.kronos.entity.InstructorSeguimiento;
import com.etapa_productiva.kronos.entity.Municipio;
import com.etapa_productiva.kronos.entity.Notificacion;
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
import com.etapa_productiva.kronos.repository.AuditoriaRepository;
import com.etapa_productiva.kronos.repository.DepartamentoRepository;
import com.etapa_productiva.kronos.repository.DocumentoSolicitudRepository;
import com.etapa_productiva.kronos.repository.EmpresaRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;
import com.etapa_productiva.kronos.repository.MunicipioRepository;
import com.etapa_productiva.kronos.repository.HistorialNovedadRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
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
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

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

    /**
     * 👨‍🎓 PASO 1: El Aprendiz inicia el proceso creando su solicitud en el sistema.
     * Ahora utiliza de forma segura el Enum para la modalidad que aspira.
     */
    @Transactional
    public SolicitudEtapaPractica aprendizCrearSolicitud(Long idAprendizFicha, Long idSeccionFormato, ModalidadEtapa modalidad) {
        // Validación de seguridad para evitar solicitudes duplicadas en proceso
        if (solicitudRepository.existsByAprendizFichaIdAprendizFichaAndEstadoNot(idAprendizFicha, EstadoSolicitud.RECHAZADO)) {
            throw new IllegalStateException("El aprendiz ya cuenta con una solicitud activa en el sistema.");
        }

        AprendizFicha aprendizFicha = aprendizFichaRepository.findById(idAprendizFicha)
                .orElseThrow(() -> new RuntimeException("Aprendiz-Ficha no encontrado con ID: " + idAprendizFicha));
        SeccionFormato seccionFormato = seccionFormatoRepository.findById(idSeccionFormato)
                .orElseThrow(() -> new RuntimeException("Modalidad de contrato no encontrada con ID: " + idSeccionFormato));

            // Fíjate en la 'S' mayúscula de nuevaSolicitud:
        SolicitudEtapaPractica nuevaSolicitud = new SolicitudEtapaPractica();
        nuevaSolicitud.setAprendizFicha(aprendizFicha);
        nuevaSolicitud.setSeccionFormato(seccionFormato);
        nuevaSolicitud.setModalidadSolicitada(modalidad); // 🚀 Ya no fallará por tipo
        nuevaSolicitud.setEstado(EstadoSolicitud.PENDIENTE_REVISION);
        nuevaSolicitud.setFechaActualizacion(LocalDateTime.now());

        SolicitudEtapaPractica guardada = solicitudRepository.save(nuevaSolicitud);

        Usuario aprendizRemitente = aprendizFicha.getUsuario();
        for (Usuario gestor : usuarioRepository.findAllGestoresEtapaActivos()) {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioDestino(gestor);
            notificacion.setMensaje("📝 " + aprendizRemitente.getNombre() + " " + aprendizRemitente.getApellido()
                    + " radicó una nueva solicitud de etapa productiva (" + modalidad + ").");
            notificacionRepository.save(notificacion);
        }

        return guardada;
    }

    /**
     * 👨‍💼 PASO 2 y 3: El Coordinador evalúa el primer filtro (Bandeja de entrada).
     * Digita los checks de Fecha Estipulada y Competencias Aprobadas en la interfaz.
     */
    @Transactional
    public SolicitudEtapaPractica coordinadorEvaluarPrimerFiltro(Long idSolicitud, boolean fechaOk, boolean competenciasOk) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE_REVISION) {
            throw new IllegalStateException("Acción denegada: La solicitud no se encuentra pendiente de revisión inicial.");
        }

        solicitud.setCheckFechaEstipulada(fechaOk);
        solicitud.setCheckCompetenciasAprobadas(competenciasOk);

        // Si cumple ambos requisitos iniciales, se le habilita el módulo de formatos
        if (fechaOk && competenciasOk) {
            solicitud.setEstado(EstadoSolicitud.FORMATOS_HABILITADOS);
        } else {
            solicitud.setEstado(EstadoSolicitud.RECHAZADO);
        }

        solicitud.setFechaActualizacion(LocalDateTime.now());
        SolicitudEtapaPractica actualizada = solicitudRepository.save(solicitud);

        if (actualizada.getEstado() == EstadoSolicitud.FORMATOS_HABILITADOS) {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioDestino(actualizada.getAprendizFicha().getUsuario());
            notificacion.setMensaje("¡Tu solicitud fue aprobada en el primer filtro! Ya puedes subir tus formatos de "
                    + actualizada.getSeccionFormato().getNombreSeccion() + " diligenciados.");
            notificacionRepository.save(notificacion);
        }

        return actualizada;
    }

    /**
     * 👨‍🎓 PASO 4: El Aprendiz descarga los documentos del formato de su modalidad,
     * los diligencia y sube el archivo radicado al sistema (nombre físico único con UUID).
     */
    @Transactional
    public SolicitudEtapaPractica aprendizSubirFormatos(Long idSolicitud, MultipartFile archivo) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.FORMATOS_HABILITADOS) {
            throw new IllegalStateException("Acción denegada: La solicitud no se encuentra en estado de carga de formatos.");
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
            String nombreArchivo = java.util.UUID.randomUUID() + extension;
            Path destino = directorio.resolve(nombreArchivo);
            archivo.transferTo(destino);

            solicitud.setRutaFormatosSubidos(rutaWeb(destino));
            solicitud.setEstado(EstadoSolicitud.FORMATOS_ENVIADOS); // Viaja a la bandeja del Gestor de Etapa
            solicitud.setFechaActualizacion(LocalDateTime.now());

            return solicitudRepository.save(solicitud);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo en el servidor: " + e.getMessage(), e);
        }
    }

    /**
     * 🗂️ PASO ADICIONAL: El Gestor de Etapa habilita el panel de descarga/resubida de
     * plantillas para el aprendiz, una vez que ya envió sus formatos iniciales.
     * No reemplaza ni altera el ESTADO de la solicitud: es una bandera independiente.
     */
    @Transactional
    public SolicitudEtapaPractica gestorHabilitarFormatos(Long idSolicitud, Long idUsuarioGestor) {
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.FORMATOS_ENVIADOS) {
            throw new IllegalStateException("Acción denegada: La solicitud aún no ha enviado sus formatos iniciales.");
        }

        if (solicitud.isPlantillasHabilitadas()) {
            throw new IllegalStateException("Las plantillas de esta solicitud ya fueron habilitadas.");
        }

        solicitud.setPlantillasHabilitadas(true);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        SolicitudEtapaPractica actualizada = solicitudRepository.save(solicitud);

        Usuario gestor = usuarioRepository.findById(idUsuarioGestor).orElse(null);
        Auditoria auditoria = Auditoria.builder()
                .usuario(gestor)
                .descripcion("Habilitó las plantillas de la solicitud #" + idSolicitud)
                .accion(AccionAuditoria.UPDATE)
                .build();
        auditoriaRepository.save(auditoria);

        Usuario aprendizDestino = actualizada.getAprendizFicha().getUsuario();
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioDestino(aprendizDestino);
        notificacion.setMensaje("¡Tus plantillas ya están habilitadas! Descárgalas, diligéncialas y firmarlas para resubirlas.");
        notificacionRepository.save(notificacion);

        return actualizada;
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

            return documentoSolicitudRepository.save(documento);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo en el servidor: " + e.getMessage(), e);
        }
    }

    /**
     * 👨‍💼 PASO 5 y 6: El Coordinador evalúa y califica los formatos, completa los checks finales,
     * cierra la solicitud e inyecta la información en TU entidad real usando el patrón Builder.
     */
    @Transactional
    public EtapaProductiva coordinadorHabilitarYAsignarEtapa(
            Long idSolicitud,
            boolean modalidadOk,
            boolean formatosOk,
            Long idAprendizFicha,              // ID de la relación ManyToOne (se resuelve contra la BD, no se confía en el body)
            Long idEmpresa,                    // ID de la relación ManyToOne
            Long idTipoContrato,                // ID de la relación ManyToOne
            LocalDate fechaInicio,             // Columna FECHA_INICIO (Imagen 2)
            LocalDate fechaFin,                // Columna FECHA_FIN (Imagen 2)
            String nombreJefeInmediato,        // Columna NOMBRE_JEFE_INMEDIATO (Imagen 3)
            String correoJefeInmediato,        // Columna CORREO_JEFE_INMEDIATO (Imagen 3)
            String telefonoJefeInmediato) {    // Columna TELEFONO_JEFE_INMEDIATO (Imagen 3)

        // 1. Validar y cerrar la solicitud intermedia del flujo de trabajo
        SolicitudEtapaPractica solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.FORMATOS_ENVIADOS) {
            throw new IllegalStateException("Acción denegada: La solicitud no se encuentra en estado de revisión final de formatos.");
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

        solicitud.setCheckModalidadAprobada(modalidadOk);
        solicitud.setCheckFormatosRadicados(formatosOk);

        if (!modalidadOk || !formatosOk) {
            solicitud.setEstado(EstadoSolicitud.RECHAZADO);
            solicitudRepository.save(solicitud);
            throw new RuntimeException("No se puede dar de alta la etapa: Los requisitos finales fueron rechazados.");
        }

        solicitud.setEstado(EstadoSolicitud.APROBADO_EN_ETAPA);
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
                .build();                                      // estadoEtapa se auto-inicializa gracias a tu @PrePersist

        // Guardar definitivamente en Oracle mediante tu repositorio oficial
        EtapaProductiva etapaGuardada = etapaProductivaRepository.save(nuevaEtapa);

        // 4. 📅 Automatización del calendario: genera las 12 bitácoras quincenales reglamentarias
        cronogramaService.generarCronograma(etapaGuardada);

        Usuario aprendizDestino = aprendizFicha.getUsuario();
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioDestino(aprendizDestino);
        notificacion.setMensaje("🎉 ¡Tu Etapa Productiva ya fue registrada y está activa! Ya puedes subir tus bitácoras y tu formato de planeación.");
        notificacionRepository.save(notificacion);

        return etapaGuardada;
    }

    /**
     * 🏢 Módulo "Registro Etapa Productiva": el Gestor de Etapa digita libremente los datos
     * de la empresa, el municipio/departamento y el tipo de contrato (sin catálogos previos).
     * Resuelve o crea cada catálogo por su clave natural y delega en el registro ya probado.
     */
    @Transactional
    public EtapaProductiva registrarEtapaProductiva(
            Long idSolicitud,
            Long idAprendizFicha,
            boolean modalidadOk,
            boolean formatosOk,
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
            String telefonoJefeInmediato) {

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

        return coordinadorHabilitarYAsignarEtapa(
                idSolicitud, modalidadOk, formatosOk, idAprendizFicha,
                empresa.getIdEmpresa(), tipoContrato.getIdTipoContrato(),
                fechaInicio, fechaFin, nombreJefeInmediato, correoJefeInmediato, telefonoJefeInmediato);
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

        Notificacion notificacionAprendiz = new Notificacion();
        notificacionAprendiz.setUsuarioDestino(aprendizDestino);
        notificacionAprendiz.setMensaje("👨‍🏫 Se te asignó a " + instructorDestino.getNombre() + " " + instructorDestino.getApellido()
                + " como tu Instructor de Seguimiento.");
        notificacionRepository.save(notificacionAprendiz);

        Notificacion notificacionInstructor = new Notificacion();
        notificacionInstructor.setUsuarioDestino(instructorDestino);
        notificacionInstructor.setMensaje("👨‍🎓 Se te asignó el seguimiento de " + aprendizDestino.getNombre() + " " + aprendizDestino.getApellido() + ".");
        notificacionRepository.save(notificacionInstructor);

        return guardada;
    }

    /**
     * 📢 El Aprendiz radica una Novedad (suspensión, aplazamiento, cambio de empresa, reclamo, etc.)
     * sobre su propia Etapa Productiva activa, eligiendo si va dirigida a su Instructor de
     * Seguimiento asignado o al Gestor de Etapa. Notifica al destinatario de inmediato.
     */
    @Transactional
    public Novedad reportarNovedad(Long idUsuarioAprendiz, Long idEtapa, TipoNovedad tipoNovedad, String descripcion, String destinatarioTipo) {
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
                .build();

        Novedad guardada = novedadRepository.save(novedad);

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioDestino(destinatario);
        notificacion.setMensaje("📢 Nueva novedad de " + remitente.getNombre() + " " + remitente.getApellido()
                + " (" + tipoNovedad + "): " + descripcion);
        notificacionRepository.save(notificacion);

        return guardada;
    }

    /**
     * 💬 El Instructor de Seguimiento o el Gestor de Etapa responde una Novedad dirigida a él,
     * dejando trazabilidad en HISTORIAL_NOVEDAD y notificando la respuesta de vuelta al aprendiz.
     */
    @Transactional
    public HistorialNovedad responderNovedad(Long idNovedad, Long idUsuarioAccion, String comentarioRespuesta, AccionRealizada accionRealizada) {
        Novedad novedad = novedadRepository.findById(idNovedad)
                .orElseThrow(() -> new RuntimeException("Novedad no encontrada con ID: " + idNovedad));

        if (!novedad.getDestinatarioAc().getIdUsuario().equals(idUsuarioAccion)) {
            throw new IllegalArgumentException("Esta novedad no está dirigida a ti.");
        }

        Usuario usuarioAccion = usuarioRepository.findById(idUsuarioAccion)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuarioAccion));

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

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioDestino(novedad.getRemitente());
        notificacion.setMensaje("💬 Respuesta a tu novedad (" + novedad.getTipoNovedad() + "): " + comentarioRespuesta);
        notificacionRepository.save(notificacion);

        return guardado;
    }

    // Convierte una ruta física local (con separadores de Windows) en una URL servible por WebConfig (/uploads/**)
    private String rutaWeb(Path destino) {
        return "/" + destino.toString().replace('\\', '/');
    }
}