package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.service.KronosWorkflowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

/**
 * 🎓 Módulo "Bandeja de Solicitudes" del rol REGISTRO: solicitudes de Contrato de Aprendizaje,
 * que llegan directo (sin pasar por el Gestor de Etapa) porque su etapa productiva ya se
 * gestiona en Sofía Plus. Sin checklist de documentos — solo novedad y Aprobar/No aprobar.
 */
@Controller
public class RegistroBandejaSolicitudesController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private KronosWorkflowService workflowService;

    @GetMapping("/registro/bandeja-solicitudes")
    public String verBandejaSolicitudes(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("REGISTRO")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        List<SolicitudEtapaPractica> solicitudesPendientes =
                solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE_REGISTRO).stream()
                        .sorted(Comparator.comparing((SolicitudEtapaPractica s) -> s.getAprendizFicha().getUsuario().getNombre(), String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(s -> s.getAprendizFicha().getUsuario().getApellido(), String.CASE_INSENSITIVE_ORDER))
                        .toList();
        model.addAttribute("solicitudesPendientes", solicitudesPendientes);

        return "registro-bandeja-solicitudes";
    }

    @PostMapping("/registro/bandeja-solicitudes/{idSolicitud}")
    public String evaluarSolicitud(
            @PathVariable Long idSolicitud,
            @RequestParam boolean aprobado,
            @RequestParam(required = false) String novedad,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("REGISTRO")) {
            redirectAttributes.addFlashAttribute("error", "Solo el rol Registro puede evaluar esta solicitud.");
            return "redirect:/index";
        }

        try {
            workflowService.registroEvaluarSolicitudContratoAprendizaje(idSolicitud, aprobado, novedad);
            redirectAttributes.addFlashAttribute("exito", aprobado
                    ? "Solicitud aprobada. Ya puedes registrar la Etapa Productiva en Contrato de Aprendizaje."
                    : "Solicitud rechazada y notificada al aprendiz.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/registro/bandeja-solicitudes";
    }
}
