package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 🎓 Módulo de solo lectura para el rol COORDINADOR_ACADEMICO: le permite ver las solicitudes
 * de Etapa Práctica radicadas por los aprendices y los checks que el Gestor de Etapa marcó en
 * el primer filtro (fecha estipulada / competencias aprobadas), sin poder modificar nada. Para
 * hablar con el Gestor de Etapa, este rol usa el chat de "Novedades" (canal COORD_ACADEMICO).
 */
@Controller
public class CoordinacionAcademicaController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @GetMapping("/coordinacion/solicitudes")
    public String verSolicitudes(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        if (usuarioLogueado.getRoles() == null || !usuarioLogueado.getRoles().contains("COORDINADOR_ACADEMICO")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("solicitudes", solicitudRepository.findAllByOrderByFechaActualizacionDesc());

        return "coordinacion-solicitudes";
    }
}
