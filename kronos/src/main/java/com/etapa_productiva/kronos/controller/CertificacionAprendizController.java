package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.AprendizCertificacionDto;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.service.CertificacionAprendizService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 🎓 Módulo "Certificación Aprendiz" del Gestor de Etapa: bandeja de aprendices que ya
 * completaron el 100% de sus requisitos (bitácoras + Formato 023) y están a la espera de la
 * aprobación final que certifica su Etapa Productiva.
 */
@Controller
public class CertificacionAprendizController {

    @Autowired
    private CertificacionAprendizService certificacionAprendizService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @GetMapping("/gestor/certificacion")
    public String verCertificacion(HttpSession session, Model model) {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("GESTOR_ETAPA")) {
            return "redirect:/index";
        }

        List<AprendizCertificacionDto> aprendices = certificacionAprendizService.listarPorCertificar();

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario()));
        model.addAttribute("aprendicesPorCertificar", aprendices);

        return "certificacion-aprendiz";
    }

    @PostMapping("/gestor/certificacion/aprobar")
    public String certificarAprendiz(
            @RequestParam Long idEtapa,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("GESTOR_ETAPA")) {
            return "redirect:/index";
        }

        try {
            certificacionAprendizService.certificar(idEtapa);
            redirectAttributes.addFlashAttribute("exito", "¡Aprendiz certificado con éxito! Se le notificó el cambio de estado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/gestor/certificacion";
    }
}
