package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.service.VisitaSeguimientoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 📍 Módulo "Mis Visitas de Seguimiento" del Aprendiz: consulta las visitas que su
 * Instructor de Seguimiento le agendó (pasadas, pendientes de hoy y futuras). Independiente
 * de "Mi Cronograma", que solo cubre las bitácoras.
 */
@Controller
public class AprendizVisitasController {

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private VisitaSeguimientoService visitaSeguimientoService;

    @GetMapping("/aprendiz/visitas")
    public String verMisVisitas(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario()).orElse(null);
        model.addAttribute("etapaActiva", etapaActiva);

        // Menú reactivo: refleja el mismo "📁 Formatos"/"Subir Bitácoras" condicional que usa /index
        // (sin duplicarlo en la sesión).
        SolicitudEtapaPractica solicitudActual = aprendizFichaRepository.findByUsuarioIdUsuario(usuarioLogueado.getIdUsuario())
                .map(AprendizFicha::getIdAprendizFicha)
                .flatMap(solicitudRepository::findByAprendizFichaIdAprendizFicha)
                .orElse(null);
        model.addAttribute("menuNavegacionActual",
                IndexController.menuAprendizReactivo(usuarioLogueado, solicitudActual, etapaActiva));
        model.addAttribute("agendaVisitas", etapaActiva != null
                ? visitaSeguimientoService.listarAgendaAprendiz(etapaActiva.getIdEtapa())
                : null);

        return "mis-visitas-aprendiz";
    }
}
