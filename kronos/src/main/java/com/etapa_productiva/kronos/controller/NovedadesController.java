package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AccionRealizada;
import com.etapa_productiva.kronos.entity.HistorialNovedad;
import com.etapa_productiva.kronos.entity.Novedad;
import com.etapa_productiva.kronos.entity.TipoNovedad;
import com.etapa_productiva.kronos.repository.HistorialNovedadRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.NovedadRepository;
import com.etapa_productiva.kronos.service.KronosWorkflowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📢 Módulo "Novedades": bandeja compartida del Instructor de Seguimiento y del Gestor de
 * Etapa con las novedades que los aprendices les radicaron directamente, con opción de
 * responder. También aloja el chat informativo GESTOR_ETAPA ↔ REGISTRO (tipo INFORMATIVO),
 * visible para todos los usuarios de esos dos roles, no solo para un destinatario fijo.
 */
@Controller
public class NovedadesController {

    @Autowired
    private NovedadRepository novedadRepository;

    @Autowired
    private HistorialNovedadRepository historialNovedadRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private KronosWorkflowService workflowService;

    @Autowired
    private com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository instructorSeguimientoRepository;

    @Autowired
    private com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @GetMapping("/novedades")
    public String verNovedades(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        boolean puedeChatRegistro = roles != null && (roles.contains("GESTOR_ETAPA") || roles.contains("REGISTRO"));
        boolean puedeChatCoordinacion = roles != null && (roles.contains("GESTOR_ETAPA") || roles.contains("COORDINADOR_ACADEMICO"));
        boolean autorizado = roles != null && (roles.contains("INSTRUCTOR_SEGUIMIENTO") || puedeChatRegistro || puedeChatCoordinacion);
        if (!autorizado) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        List<Novedad> novedades = new ArrayList<>(
                novedadRepository.findByDestinatarioAcIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        // 💬 Chat INSTRUCTOR_SEGUIMIENTO → APRENDIZ: el instructor también ve los mensajes que
        // él mismo envió a sus aprendices (es el remitente), para seguir la conversación.
        boolean esInstructorSeguimiento = roles.contains("INSTRUCTOR_SEGUIMIENTO");
        model.addAttribute("puedeChatAprendiz", esInstructorSeguimiento);
        if (esInstructorSeguimiento) {
            for (Novedad enviada : novedadRepository.findByRemitenteIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario())) {
                if (novedades.stream().noneMatch(n -> n.getIdNovedad().equals(enviada.getIdNovedad()))) {
                    novedades.add(enviada);
                }
            }
            // Sus aprendices con asignación vigente, para el select del formulario de mensaje
            model.addAttribute("aprendicesAsignados", instructorSeguimientoRepository
                    .findByUsuarioIdUsuario(usuarioLogueado.getIdUsuario())
                    .map(instructor -> asignacionInstructorEtapaRepository
                            .findByInstructorIdInstructorSeguimientoAndEstadoAsignacionTrue(instructor.getIdInstructorSeguimiento()))
                    .orElse(new ArrayList<>()));
        }

        // 💬 El chat GESTOR_ETAPA ↔ REGISTRO es visible para todo el rol, no solo para el
        // destinatario puntual que quedó guardado en cada mensaje.
        model.addAttribute("puedeChatRegistro", puedeChatRegistro);
        if (puedeChatRegistro) {
            for (Novedad informativa : novedadRepository.findByTipoNovedadOrderByFechaCreacionAsc(TipoNovedad.INFORMATIVO)) {
                if (novedades.stream().noneMatch(n -> n.getIdNovedad().equals(informativa.getIdNovedad()))) {
                    novedades.add(informativa);
                }
            }
        }

        // 💬 El chat GESTOR_ETAPA ↔ COORDINADOR_ACADEMICO es un canal aparte del de Registro:
        // cada rol solo ve los mensajes de su propio canal, el Gestor ve ambos.
        model.addAttribute("puedeChatCoordinacion", puedeChatCoordinacion);
        if (puedeChatCoordinacion) {
            for (Novedad coordinacion : novedadRepository.findByTipoNovedadOrderByFechaCreacionAsc(TipoNovedad.COORD_ACADEMICO)) {
                if (novedades.stream().noneMatch(n -> n.getIdNovedad().equals(coordinacion.getIdNovedad()))) {
                    novedades.add(coordinacion);
                }
            }
        }

        Map<Long, List<HistorialNovedad>> historialPorNovedad = new HashMap<>();
        for (Novedad novedad : novedades) {
            historialPorNovedad.put(novedad.getIdNovedad(),
                    historialNovedadRepository.findByNovedadIdNovedadOrderByFechaAccionAsc(novedad.getIdNovedad()));
        }

        novedades.sort(Comparator.comparing(Novedad::getFechaCreacion).reversed());
        model.addAttribute("novedades", novedades);
        model.addAttribute("historialPorNovedad", historialPorNovedad);
        model.addAttribute("accionesRealizadas", AccionRealizada.values());

        return "novedades";
    }

    @PostMapping("/novedades/informativa")
    public String enviarInformativa(
            @RequestParam(required = false) String mensaje,
            @RequestParam(required = false) MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        boolean autorizado = roles != null && (roles.contains("GESTOR_ETAPA") || roles.contains("REGISTRO"));
        if (!autorizado) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para enviar mensajes en este canal.");
            return "redirect:/index";
        }

        try {
            workflowService.enviarNovedadInformativa(usuarioLogueado.getIdUsuario(), mensaje, archivo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/novedades";
    }

    @PostMapping("/novedades/coordinacion")
    public String enviarCoordinacion(
            @RequestParam(required = false) String mensaje,
            @RequestParam(required = false) MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        boolean autorizado = roles != null && (roles.contains("GESTOR_ETAPA") || roles.contains("COORDINADOR_ACADEMICO"));
        if (!autorizado) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para enviar mensajes en este canal.");
            return "redirect:/index";
        }

        try {
            workflowService.enviarNovedadCoordinacion(usuarioLogueado.getIdUsuario(), mensaje, archivo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/novedades";
    }

    /**
     * 💬 El Instructor de Seguimiento envía un mensaje (con adjunto opcional) a uno de sus
     * aprendices asignados; queda como Novedad de su Etapa Productiva y se notifica al aprendiz.
     */
    @PostMapping("/novedades/aprendiz")
    public String enviarMensajeAprendiz(
            @RequestParam Long idEtapa,
            @RequestParam(required = false) String mensaje,
            @RequestParam(required = false) MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            redirectAttributes.addFlashAttribute("error", "Solo el Instructor de Seguimiento puede enviar mensajes a sus aprendices.");
            return "redirect:/index";
        }

        try {
            workflowService.instructorEnviarNovedadAprendiz(usuarioLogueado.getIdUsuario(), idEtapa, mensaje, archivo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/novedades";
    }

    @PostMapping("/novedades/{idNovedad}/responder")
    public String responderNovedad(
            @PathVariable Long idNovedad,
            @RequestParam String comentarioRespuesta,
            @RequestParam AccionRealizada accionRealizada,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        boolean autorizado = roles != null && (roles.contains("INSTRUCTOR_SEGUIMIENTO")
                || roles.contains("GESTOR_ETAPA") || roles.contains("REGISTRO") || roles.contains("COORDINADOR_ACADEMICO"));
        if (!autorizado) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para responder novedades.");
            return "redirect:/index";
        }

        try {
            workflowService.responderNovedad(idNovedad, usuarioLogueado.getIdUsuario(), comentarioRespuesta, accionRealizada);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/novedades";
    }
}
