package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.MenuDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.InstructorSeguimiento;
import com.etapa_productiva.kronos.entity.InstructorTecnico;
import com.etapa_productiva.kronos.entity.ModalidadEtapa;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.entity.TipoNovedad;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.DocumentoSolicitudRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoFichaRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;
import com.etapa_productiva.kronos.repository.InstructorTecnicoFichaRepository;
import com.etapa_productiva.kronos.repository.InstructorTecnicoRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.NovedadRepository;
import com.etapa_productiva.kronos.repository.PlantillaFormatoRepository;
import com.etapa_productiva.kronos.repository.SeccionFormatoRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.service.KronosWorkflowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private InstructorSeguimientoRepository instructorSeguimientoRepository;

    @Autowired
    private InstructorSeguimientoFichaRepository instructorSeguimientoFichaRepository;

    @Autowired
    private InstructorTecnicoRepository instructorTecnicoRepository;

    @Autowired
    private InstructorTecnicoFichaRepository instructorTecnicoFichaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private KronosWorkflowService workflowService;

    @Autowired
    private SeccionFormatoRepository seccionFormatoRepository;

    @Autowired
    private PlantillaFormatoRepository plantillaFormatoRepository;

    @Autowired
    private DocumentoSolicitudRepository documentoSolicitudRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private NovedadRepository novedadRepository;

    @GetMapping("/index")
    public String verIndex(HttpSession session, Model model) {
        // 1. Validar seguridad: Si no hay sesión activa, va para el login
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");

        if (usuarioLogueado == null) {
            System.out.println("🛑 [INDEX] Intento de acceso sin login. Redirigiendo a Login...");
            return "redirect:/auth/login";
        }

        // 2. Pasar los datos del usuario al HTML para pintar el menú dinámico
        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        List<String> roles = usuarioLogueado.getRoles();

        // 3 y 4. Bandeja del Gestor de Etapa: absorbe lo que antes era exclusivo del Coordinador
        //    (solicitudes pendientes) + lo que ya le pertenecía (documentos por validar,
        //    solicitudes que ya enviaron formatos y esperan que se les habiliten las plantillas)
        if (roles != null && roles.contains("GESTOR_ETAPA")) {
            model.addAttribute("solicitudesPendientes",
                    solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE_REVISION));
            model.addAttribute("documentosPendientes",
                    documentoSolicitudRepository.findByEstadoValidacion(EstadoValidacion.PENDIENTE));
            model.addAttribute("solicitudesParaHabilitarFormatos",
                    solicitudRepository.findByEstadoAndPlantillasHabilitadas(EstadoSolicitud.FORMATOS_ENVIADOS, false));
        } else {
            model.addAttribute("solicitudesPendientes", Collections.emptyList());
            model.addAttribute("documentosPendientes", Collections.emptyList());
            model.addAttribute("solicitudesParaHabilitarFormatos", Collections.emptyList());
        }

        // 5. Panel del Instructor de Seguimiento: fichas activas que tiene asignadas
        if (roles != null && roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            model.addAttribute("fichasSeguimiento", buscarFichasSeguimiento(usuarioLogueado.getIdUsuario()));
        } else {
            model.addAttribute("fichasSeguimiento", Collections.emptyList());
        }

        // 6. Panel del Instructor Técnico: fichas activas que tiene asignadas para revisión
        if (roles != null && roles.contains("INSTRUCTOR_TECNICO")) {
            model.addAttribute("fichasTecnico", buscarFichasTecnico(usuarioLogueado.getIdUsuario()));
        } else {
            model.addAttribute("fichasTecnico", Collections.emptyList());
        }

        // 7. Panel del Administrador: conteo global de usuarios del sistema
        if (roles != null && roles.contains("ADMINISTRADOR")) {
            model.addAttribute("totalUsuarios", usuarioRepository.count());
            model.addAttribute("usuariosActivos", usuarioRepository.countByEstado(true));
            model.addAttribute("usuariosInactivos", usuarioRepository.countByEstado(false));
        }

        // 8. Panel del Aprendiz: su solicitud actual (o el formulario para radicar una nueva si no tiene)
        //    + si el Gestor de Etapa ya habilitó las plantillas, el panel de descarga/resubida
        //    Se calcula un menú fresco en cada request (no se muta el de la sesión) para que la
        //    opción "Mis Formatos" aparezca/desaparezca de forma reactiva según el ESTADO real en Oracle.
        List<MenuDto> menuNavegacionActual = new ArrayList<>(
                usuarioLogueado.getMenuNavegacion() != null ? usuarioLogueado.getMenuNavegacion() : Collections.emptyList());

        if (roles != null && roles.contains("APRENDIZ")) {
            SolicitudEtapaPractica solicitudActual = buscarSolicitudActual(usuarioLogueado.getIdUsuario());
            model.addAttribute("solicitudActual", solicitudActual);
            model.addAttribute("seccionesFormato", seccionFormatoRepository.findByEstadoTrue());

            // El módulo "Formatos" solo aparece en el sidebar una vez el Gestor de Etapa
            // aprobó el primer filtro (fecha/competencias) y habilitó los formatos.
            if (formatosDesbloqueados(solicitudActual)) {
                menuNavegacionActual.add(new MenuDto("📁 Formatos", "/formatos"));
            }

            // Las plantillas de descarga por tipo de contrato ahora viven en el módulo /formatos;
            // aquí solo se recargan para el ciclo de resubida que habilita el Gestor de Etapa.
            if (solicitudActual != null && solicitudActual.isPlantillasHabilitadas()) {
                model.addAttribute("plantillasDescarga",
                        plantillaFormatoRepository.findWordExcelPorSeccion(solicitudActual.getSeccionFormato().getIdSeccionFormato()));
            } else {
                model.addAttribute("plantillasDescarga", Collections.emptyList());
            }

            if (solicitudActual != null && solicitudActual.isPlantillasHabilitadas()) {
                model.addAttribute("documentosSubidos",
                        documentoSolicitudRepository.findBySolicitudIdSolicitud(solicitudActual.getIdSolicitud()));
            } else {
                model.addAttribute("documentosSubidos", Collections.emptyList());
            }

            // Timeline (Mes 1 a Mes 6) calculado a partir de las fechas reales de la Etapa Productiva
            EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario()).orElse(null);
            model.addAttribute("etapaActiva", etapaActiva);
            model.addAttribute("mesesEtapa", calcularMesesEtapa(etapaActiva));
            model.addAttribute("progresoEtapaPorcentaje", calcularProgresoEtapa(etapaActiva));

            // Novedades radicadas por el aprendiz sobre su propia Etapa Productiva
            model.addAttribute("novedades",
                    novedadRepository.findByRemitenteIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));
            model.addAttribute("tiposNovedad", TipoNovedad.values());

            // Para que el formulario de Novedades permita elegir el Instructor de Seguimiento asignado (si ya tiene uno)
            model.addAttribute("instructorAsignado", etapaActiva == null ? null :
                    asignacionInstructorEtapaRepository.findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(etapaActiva.getIdEtapa())
                            .map(a -> a.getInstructor().getUsuario())
                            .orElse(null));
        } else {
            model.addAttribute("solicitudActual", null);
            model.addAttribute("seccionesFormato", Collections.emptyList());
            model.addAttribute("plantillasDescarga", Collections.emptyList());
            model.addAttribute("documentosSubidos", Collections.emptyList());
            model.addAttribute("etapaActiva", null);
            model.addAttribute("mesesEtapa", Collections.emptyList());
            model.addAttribute("progresoEtapaPorcentaje", 0.0);
            model.addAttribute("novedades", Collections.emptyList());
            model.addAttribute("tiposNovedad", TipoNovedad.values());
            model.addAttribute("instructorAsignado", null);
        }

        model.addAttribute("menuNavegacionActual", menuNavegacionActual);

        System.out.println("🚀 [INDEX] Renderizando el panel principal (index) para: " + usuarioLogueado.getNombre());

        // Retorna el nombre exacto de tu nuevo archivo: index.html
        return "index";
    }

    /**
     * 👨‍🎓 El Aprendiz radica su solicitud inicial desde el formulario adaptativo de /index.
     * POST http://localhost:8080/aprendiz/solicitar
     */
    @PostMapping("/aprendiz/solicitar")
    public String crearSolicitudAprendiz(
            @RequestParam Long idSeccionFormato,
            @RequestParam ModalidadEtapa modalidad,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            redirectAttributes.addFlashAttribute("error", "Solo un aprendiz puede radicar una solicitud de etapa práctica.");
            return "redirect:/index";
        }

        try {
            AprendizFicha aprendizFicha = aprendizFichaRepository.findByUsuarioIdUsuario(usuarioLogueado.getIdUsuario())
                    .orElseThrow(() -> new IllegalStateException("No se encontró una matrícula de aprendiz asociada a tu usuario."));

            workflowService.aprendizCrearSolicitud(aprendizFicha.getIdAprendizFicha(), idSeccionFormato, modalidad);

            System.out.println("🚀 [INDEX] Solicitud de etapa práctica radicada por: " + usuarioLogueado.getNombre());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        // Redirige a /index para refrescar el modelo: la solicitud recién creada
        // ya aparecerá en la bandeja de pendientes del Coordinador Académico.
        return "redirect:/index";
    }

    /**
     * 👨‍🎓 El Aprendiz sube sus formatos iniciales diligenciados una vez el Gestor de Etapa
     * aprobó los primeros checks (fecha/competencias). Mueve la solicitud a FORMATOS_ENVIADOS,
     * lo que la hace visible en la bandeja "Solicitudes para Habilitar Plantillas" del Gestor.
     * POST /aprendiz/subir-formatos
     */
    @PostMapping(value = "/aprendiz/subir-formatos", consumes = "multipart/form-data")
    public String subirFormatosAprendiz(
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            redirectAttributes.addFlashAttribute("error", "Solo un aprendiz puede subir sus formatos.");
            return "redirect:/index";
        }

        try {
            SolicitudEtapaPractica solicitudActual = buscarSolicitudActual(usuarioLogueado.getIdUsuario());
            if (solicitudActual == null) {
                throw new IllegalStateException("No tienes una solicitud de etapa práctica en curso.");
            }

            workflowService.aprendizSubirFormatos(solicitudActual.getIdSolicitud(), archivo);

            System.out.println("🚀 [INDEX] Formatos iniciales subidos por: " + usuarioLogueado.getNombre());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/formatos";
    }

    /**
     * 👨‍🎓 El Aprendiz resube una plantilla diligenciada y firmada (multipart/form-data).
     * POST http://localhost:8080/aprendiz/subir-plantilla
     */
    @PostMapping(value = "/aprendiz/subir-plantilla", consumes = "multipart/form-data")
    public String subirPlantillaAprendiz(
            @RequestParam Long idPlantilla,
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            redirectAttributes.addFlashAttribute("error", "Solo un aprendiz puede subir sus plantillas firmadas.");
            return "redirect:/index";
        }

        try {
            SolicitudEtapaPractica solicitudActual = buscarSolicitudActual(usuarioLogueado.getIdUsuario());
            if (solicitudActual == null) {
                throw new IllegalStateException("No tienes una solicitud de etapa práctica en curso.");
            }

            workflowService.aprendizSubirPlantillaFirmada(solicitudActual.getIdSolicitud(), idPlantilla, archivo);

            System.out.println("🚀 [INDEX] Plantilla firmada subida por: " + usuarioLogueado.getNombre());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/index";
    }

    /**
     * 📢 El Aprendiz radica una Novedad (suspensión, aplazamiento, cambio de empresa, etc.)
     * sobre su Etapa Productiva activa.
     * POST /aprendiz/novedad
     */
    @PostMapping("/aprendiz/novedad")
    public String reportarNovedad(
            @RequestParam TipoNovedad tipoNovedad,
            @RequestParam String descripcion,
            @RequestParam String destinatarioTipo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            redirectAttributes.addFlashAttribute("error", "Solo un aprendiz puede radicar novedades.");
            return "redirect:/index";
        }

        try {
            EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario())
                    .orElseThrow(() -> new IllegalStateException("No tienes una Etapa Productiva activa."));

            workflowService.reportarNovedad(usuarioLogueado.getIdUsuario(), etapaActiva.getIdEtapa(), tipoNovedad, descripcion, destinatarioTipo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/index";
    }

    /**
     * 🔔 Marca como leídas todas las notificaciones pendientes del usuario logueado
     * (se dispara al abrir la campana de la barra superior).
     * POST /notificaciones/marcar-leidas
     */
    @PostMapping("/notificaciones/marcar-leidas")
    public String marcarNotificacionesLeidas(HttpSession session) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<com.etapa_productiva.kronos.entity.Notificacion> pendientes =
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario());
        pendientes.forEach(n -> n.setLeido(true));
        notificacionRepository.saveAll(pendientes);

        return "redirect:/index";
    }

    private SolicitudEtapaPractica buscarSolicitudActual(Long idUsuario) {
        return aprendizFichaRepository.findByUsuarioIdUsuario(idUsuario)
                .flatMap(af -> solicitudRepository.findByAprendizFichaIdAprendizFicha(af.getIdAprendizFicha()))
                .orElse(null);
    }

    // Calcula, a partir de FECHA_INICIO/FECHA_FIN reales de la Etapa Productiva, qué meses (1 a 6)
    // ya quedaron atrás según la fecha de hoy. Cada posición representa un mes del proceso.
    private List<Boolean> calcularMesesEtapa(EtapaProductiva etapa) {
        double progreso = calcularProgresoEtapa(etapa);
        List<Boolean> meses = new ArrayList<>();
        for (int mes = 1; mes <= 6; mes++) {
            meses.add(progreso >= (mes * 100.0 / 6));
        }
        return meses;
    }

    // Porcentaje de avance (0-100) entre FECHA_INICIO y FECHA_FIN de la Etapa Productiva, según la fecha de hoy.
    private double calcularProgresoEtapa(EtapaProductiva etapa) {
        if (etapa == null) {
            return 0.0;
        }
        long totalDias = Math.max(1, ChronoUnit.DAYS.between(etapa.getFechaInicio(), etapa.getFechaFin()));
        long transcurridos = ChronoUnit.DAYS.between(etapa.getFechaInicio(), LocalDate.now());
        transcurridos = Math.max(0, Math.min(transcurridos, totalDias));
        return transcurridos * 100.0 / totalDias;
    }

    // El módulo de Formatos se desbloquea para el Aprendiz una vez el Gestor de Etapa
    // aprobó el primer filtro (fecha/competencias): cualquier estado posterior a PENDIENTE_REVISION,
    // salvo que la solicitud haya sido rechazada.
    static boolean formatosDesbloqueados(SolicitudEtapaPractica solicitud) {
        return solicitud != null
                && solicitud.getEstado() != EstadoSolicitud.PENDIENTE_REVISION
                && solicitud.getEstado() != EstadoSolicitud.RECHAZADO;
    }

    private List<com.etapa_productiva.kronos.entity.InstructorSeguimientoFicha> buscarFichasSeguimiento(Long idUsuario) {
        return instructorSeguimientoRepository.findByUsuarioIdUsuario(idUsuario)
                .map(InstructorSeguimiento::getIdInstructorSeguimiento)
                .map(instructorSeguimientoFichaRepository::findByInstructorSeguimientoIdInstructorSeguimientoAndEstadoTrue)
                .orElse(Collections.emptyList());
    }

    private List<com.etapa_productiva.kronos.entity.InstructorTecnicoFicha> buscarFichasTecnico(Long idUsuario) {
        return instructorTecnicoRepository.findByUsuarioIdUsuario(idUsuario)
                .map(InstructorTecnico::getIdInstructorTecnico)
                .map(instructorTecnicoFichaRepository::findByInstructorTecnicoIdInstructorTecnicoAndEstadoTrue)
                .orElse(Collections.emptyList());
    }
}