package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.MenuDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.entity.EstadoEtapa;
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

    @Autowired
    private com.etapa_productiva.kronos.service.InstructorSeguimientoService instructorSeguimientoService;

    @Autowired
    private com.etapa_productiva.kronos.service.InstructorTecnicoService instructorTecnicoService;

    @Autowired
    private com.etapa_productiva.kronos.repository.UsuarioRolRepository usuarioRolRepository;

    @Autowired
    private com.etapa_productiva.kronos.repository.FichaRepository fichaRepository;

    @Autowired
    private com.etapa_productiva.kronos.service.EmailService emailService;

    @Autowired
    private com.etapa_productiva.kronos.service.EvaluacionFormatosService evaluacionFormatosService;

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
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

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

        // 5. Panel del Instructor de Seguimiento: dashboard (números + gráficas) de sus aprendices asignados
        if (roles != null && roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            model.addAttribute("fichasSeguimiento", buscarFichasSeguimiento(usuarioLogueado.getIdUsuario()));
            List<com.etapa_productiva.kronos.dto.InstructorAprendizDto> misAprendices =
                    instructorSeguimientoService.listarAprendices(usuarioLogueado.getIdUsuario());
            model.addAttribute("dashInstructor", instructorSeguimientoService.calcularDashboard(misAprendices));
        } else {
            model.addAttribute("fichasSeguimiento", Collections.emptyList());
            model.addAttribute("dashInstructor", null);
        }

        // 6. Panel del Instructor Técnico (líder de ficha): sus fichas + dashboard gráfico
        //    de sus aprendices (en etapa práctica, sin etapa, por certificar, certificados)
        if (roles != null && roles.contains("INSTRUCTOR_TECNICO")) {
            model.addAttribute("fichasTecnico", buscarFichasTecnico(usuarioLogueado.getIdUsuario()));
            List<com.etapa_productiva.kronos.dto.TecnicoAprendizDto> aprendicesTecnico =
                    instructorTecnicoService.listarAprendices(usuarioLogueado.getIdUsuario());
            model.addAttribute("dashTecnico", instructorTecnicoService.calcularDashboard(aprendicesTecnico));
        } else {
            model.addAttribute("fichasTecnico", Collections.emptyList());
            model.addAttribute("dashTecnico", null);
        }

        // 7. Panel de Control del Administrador: vista general del sistema
        //    (aprendices activos, en etapa práctica y sin ella, fichas, instructores y servidores)
        if (roles != null && roles.contains("ADMINISTRADOR")) {
            model.addAttribute("totalUsuarios", usuarioRepository.count());
            model.addAttribute("usuariosActivos", usuarioRepository.countByEstado(true));
            model.addAttribute("usuariosInactivos", usuarioRepository.countByEstado(false));

            long aprendicesActivos = usuarioRolRepository.countByRolNombreRolAndUsuarioEstadoTrue("APRENDIZ");
            long enEtapaPractica = etapaProductivaRepository.countByEstadoEtapa(com.etapa_productiva.kronos.entity.EstadoEtapa.EN_PROGRESO);
            model.addAttribute("adminAprendicesActivos", aprendicesActivos);
            model.addAttribute("adminEnEtapaPractica", enEtapaPractica);
            model.addAttribute("adminSinEtapaPractica", Math.max(0, aprendicesActivos - enEtapaPractica));
            model.addAttribute("adminFichasRegistradas", fichaRepository.count());
            model.addAttribute("adminInstructoresAsignados", asignacionInstructorEtapaRepository.countInstructoresConAsignacionActiva());

            // Estado de los servidores: la BD se verifica con un ping real a Oracle;
            // la app está en línea por definición (si no, esta vista ni se renderiza).
            model.addAttribute("adminBdEnLinea", verificarBaseDatos());
            model.addAttribute("adminCorreoHabilitado", emailService.estaHabilitado());
            model.addAttribute("adminUptime", calcularUptime());

            // 👋 Saludo dinámico del Administrador según la franja horaria (mañana/tarde/noche)
            String[] saludo = saludoAdministrador(usuarioLogueado.getNombre());
            model.addAttribute("saludoAdminTitulo", saludo[0]);
            model.addAttribute("saludoAdminFrase", saludo[1]);
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

            // Timeline (Mes 1 a Mes 6) calculado a partir de las fechas reales de la Etapa Productiva.
            // Se adelanta la consulta porque el menú reactivo (Formatos/Subir Bitácoras) depende de su estado.
            EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario()).orElse(null);
            model.addAttribute("etapaActiva", etapaActiva);
            model.addAttribute("mesesEtapa", calcularMesesEtapa(etapaActiva));
            model.addAttribute("progresoEtapaPorcentaje", calcularProgresoEtapa(etapaActiva));

            // 🥳 Al completar el 100% de bitácoras + Formato 023, la etapa pasa a POR_CERTIFICAR y
            // el dashboard del aprendiz se reemplaza por la tarjeta de éxito; cuando el Gestor de
            // Etapa la certifica, pasa a CERTIFICADO con su propia tarjeta. En ambos casos
            // desaparecen del sidebar "Subir Bitácoras"/"📁 Formatos" y del dashboard la solicitud
            // y la línea de tiempo.
            boolean certificando = etapaCertificando(etapaActiva);
            model.addAttribute("mostrarCertificacion", certificando);
            model.addAttribute("etapaCertificada", etapaActiva != null && etapaActiva.getEstadoEtapa() == EstadoEtapa.CERTIFICADO);
            model.addAttribute("infoCertificacion", certificando ? evaluacionFormatosService.calcularInfoCertificacion(etapaActiva) : null);

            // El módulo "Formatos" solo aparece en el sidebar una vez el Gestor de Etapa aprobó
            // el primer filtro y habilitó los formatos; todos los ítems de la etapa en curso
            // (Cronograma, Visitas, Bitácoras) desaparecen una vez el aprendiz certificó.
            menuNavegacionActual = menuAprendizReactivo(usuarioLogueado, solicitudActual, etapaActiva);

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

            // Novedades radicadas por el aprendiz sobre su propia Etapa Productiva
            model.addAttribute("novedades",
                    novedadRepository.findByRemitenteIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));
            model.addAttribute("tiposNovedad", TipoNovedad.values());

            // Para que el formulario de Novedades permita elegir el Instructor de Seguimiento asignado
            // (si ya tiene uno). Una vez CERTIFICADO ese instructor pertenece al ciclo ya cerrado,
            // así que no se ofrece hasta que el Gestor de Etapa registre una nueva Etapa Productiva.
            model.addAttribute("instructorAsignado", (etapaActiva == null || etapaActiva.getEstadoEtapa() == EstadoEtapa.CERTIFICADO) ? null :
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
            model.addAttribute("mostrarCertificacion", false);
            model.addAttribute("etapaCertificada", false);
            model.addAttribute("infoCertificacion", null);
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
    public String marcarNotificacionesLeidas(HttpSession session, jakarta.servlet.http.HttpServletRequest request) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<com.etapa_productiva.kronos.entity.Notificacion> pendientes =
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario());
        pendientes.forEach(n -> n.setLeido(true));
        notificacionRepository.saveAll(pendientes);

        // Vuelve a la página desde la que se marcaron como leídas (el historial completo
        // permanece visible en la campana, así que no hace falta forzar la vuelta a /index).
        // Se valida que el Referer sea del mismo host y solo se usa su path+query, para
        // evitar una redirección abierta hacia un sitio externo.
        String destino = "/index";
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(referer);
                if (uri.getHost() == null || uri.getHost().equalsIgnoreCase(request.getServerName())) {
                    String path = uri.getRawPath();
                    if (path != null && !path.isBlank()) {
                        destino = path + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Referer malformado: se mantiene /index por defecto
            }
        }
        return "redirect:" + destino;
    }

    // 👋 Saludo del Administrador por franja horaria, alternando entre dos frases por franja:
    //    🌅 Mañana (05:00-11:59) · ☀️ Tarde (12:00-17:59) · 🌙 Noche (18:00-04:59)
    //    Devuelve [título del h1, frase del subtítulo].
    private String[] saludoAdministrador(String nombre) {
        int hora = java.time.LocalTime.now().getHour();
        boolean primeraVariante = new java.util.Random().nextBoolean();

        if (hora >= 5 && hora < 12) { // 🌅 Mañana
            return primeraVariante
                    ? new String[]{"🌅 ¡Buenos días, " + nombre + "!",
                            "El motor de KRONOS está encendido. ¿Qué procesos optimizaremos hoy?"}
                    : new String[]{"🌅 ¡Un nuevo día de control, " + nombre + "!",
                            "El Job de la madrugada se ejecutó con éxito. Todo bajo control."};
        }
        if (hora >= 12 && hora < 18) { // ☀️ Tarde
            return primeraVariante
                    ? new String[]{"☀️ ¡Buenas tardes, " + nombre + "!",
                            "Las solicitudes y bitácoras siguen fluyendo. Revisemos el estado del centro."}
                    : new String[]{"☀️ ¡Hola, " + nombre + "!",
                            "Monitoreando la asignación masiva en tiempo real. Tu panel está actualizado."};
        }
        // 🌙 Noche (18:00 - 04:59)
        return primeraVariante
                ? new String[]{"🌙 ¡Buenas noches, " + nombre + "!",
                        "Modo nocturno activo. Vigilando la estabilidad de la plataforma."}
                : new String[]{"🌙 ¡Buen descanso, " + nombre + "!",
                        "El sistema se prepara para el corte automático de la 1:00 a.m."};
    }

    // 🩺 Ping real a Oracle para el semáforo de "Estado de los Servidores" del Administrador
    private boolean verificarBaseDatos() {
        try {
            usuarioRepository.count();
            return true;
        } catch (Exception e) {
            System.out.println("🛑 [ADMIN] La base de datos no respondió al ping: " + e.getMessage());
            return false;
        }
    }

    // ⏱️ Tiempo que lleva encendida la aplicación (Ej: "3 h 25 min"), para la tarjeta de servidores
    private String calcularUptime() {
        long minutosTotales = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 60000;
        long horas = minutosTotales / 60;
        long minutos = minutosTotales % 60;
        return horas > 0 ? horas + " h " + minutos + " min" : minutos + " min";
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

    // 🥳 El aprendiz completó su proceso: su Etapa Productiva ya está POR_CERTIFICAR (esperando
    // al Gestor de Etapa) o CERTIFICADO (ya aprobada). En ambos casos su dashboard cambia por
    // completo (ver /index) y desaparecen del sidebar "Subir Bitácoras" y "📁 Formatos".
    static boolean etapaCertificando(EtapaProductiva etapa) {
        return etapa != null
                && (etapa.getEstadoEtapa() == EstadoEtapa.POR_CERTIFICAR || etapa.getEstadoEtapa() == EstadoEtapa.CERTIFICADO);
    }

    // Ítems del sidebar exclusivos del proceso de etapa práctica en curso: una vez el aprendiz
    // completa su certificación (POR_CERTIFICAR/CERTIFICADO) ya no tienen sentido y desaparecen,
    // dejando solo "Inicio" (fijo) y "👤 Mi Perfil" (se agrega para todos los roles en AuthService).
    private static final List<String> ITEMS_ETAPA_EN_CURSO = List.of(
            "Mi Cronograma", "Mis Visitas de Seguimiento", "Subir Bitácoras");

    // Menú reactivo compartido por todas las páginas del Aprendiz: agrega "📁 Formatos" cuando
    // corresponde, o retira todos los ítems de la etapa en curso una vez el aprendiz certificó.
    static List<MenuDto> menuAprendizReactivo(LoginResponse usuario, SolicitudEtapaPractica solicitudActual, EtapaProductiva etapaActiva) {
        List<MenuDto> menu = new ArrayList<>(usuario.getMenuNavegacion() != null ? usuario.getMenuNavegacion() : Collections.emptyList());
        if (etapaCertificando(etapaActiva)) {
            menu.removeIf(m -> ITEMS_ETAPA_EN_CURSO.contains(m.getNombre()));
        } else if (formatosDesbloqueados(solicitudActual)) {
            menu.add(new MenuDto("Formatos", "/formatos"));
        }
        return menu;
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