package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.EstadoEtapa;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
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

import java.util.List;

/**
 * 👥 Módulo "Asignar Instructores" del Gestor de Etapa: asigna un Instructor de Seguimiento
 * a cada aprendiz individual cuya Etapa Productiva ya está activa y aún no tiene uno.
 */
@Controller
public class AsignarInstructorController {

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private InstructorSeguimientoRepository instructorSeguimientoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private KronosWorkflowService workflowService;

    @GetMapping("/coordinador/asignaciones")
    public String verAsignaciones(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("GESTOR_ETAPA")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("instructores", instructorSeguimientoRepository.findAll());
        model.addAttribute("etapasSinInstructor",
                etapaProductivaRepository.findSinInstructorSeguimientoAsignado(EstadoEtapa.EN_PROGRESO));

        return "asignar-instructor";
    }

    @PostMapping("/coordinador/asignaciones/{idEtapa}")
    public String asignarInstructor(
            @PathVariable Long idEtapa,
            @RequestParam Long idInstructorSeguimiento,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("GESTOR_ETAPA")) {
            redirectAttributes.addFlashAttribute("error", "Solo un Gestor de Etapa puede asignar instructores.");
            return "redirect:/index";
        }

        try {
            workflowService.asignarInstructorSeguimiento(idEtapa, idInstructorSeguimiento);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/coordinador/asignaciones";
    }
}
