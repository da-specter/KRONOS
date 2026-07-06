package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AccionRealizada;
import com.etapa_productiva.kronos.entity.HistorialNovedad;
import com.etapa_productiva.kronos.entity.Novedad;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📢 Módulo "Novedades": bandeja compartida del Instructor de Seguimiento y del Gestor de
 * Etapa con las novedades que los aprendices les radicaron directamente, con opción de responder.
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

    @GetMapping("/novedades")
    public String verNovedades(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        boolean autorizado = roles != null && (roles.contains("INSTRUCTOR_SEGUIMIENTO") || roles.contains("GESTOR_ETAPA"));
        if (!autorizado) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        List<Novedad> novedades = novedadRepository.findByDestinatarioAcIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario());
        model.addAttribute("novedades", novedades);

        Map<Long, List<HistorialNovedad>> historialPorNovedad = new HashMap<>();
        for (Novedad novedad : novedades) {
            historialPorNovedad.put(novedad.getIdNovedad(),
                    historialNovedadRepository.findByNovedadIdNovedadOrderByFechaAccionAsc(novedad.getIdNovedad()));
        }
        model.addAttribute("historialPorNovedad", historialPorNovedad);
        model.addAttribute("accionesRealizadas", AccionRealizada.values());

        return "novedades";
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
        boolean autorizado = roles != null && (roles.contains("INSTRUCTOR_SEGUIMIENTO") || roles.contains("GESTOR_ETAPA"));
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
