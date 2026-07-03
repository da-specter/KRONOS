package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.MenuDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.service.CronogramaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 📅 Módulo "Mi Cronograma" del Aprendiz: consulta de las 12 fechas límite quincenales
 * de bitácoras generadas automáticamente al activarse su Etapa Productiva.
 */
@Controller
public class CronogramaController {

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private CronogramaService cronogramaService;

    @GetMapping("/aprendiz/cronograma")
    public String verCronograma(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        // Menú reactivo: refleja el mismo "📁 Formatos" condicional que usa /index (sin duplicarlo en la sesión).
        SolicitudEtapaPractica solicitudActual = aprendizFichaRepository.findByUsuarioIdUsuario(usuarioLogueado.getIdUsuario())
                .map(AprendizFicha::getIdAprendizFicha)
                .flatMap(solicitudRepository::findByAprendizFichaIdAprendizFicha)
                .orElse(null);
        List<MenuDto> menuNavegacionActual = new ArrayList<>(
                usuarioLogueado.getMenuNavegacion() != null ? usuarioLogueado.getMenuNavegacion() : Collections.emptyList());
        if (IndexController.formatosDesbloqueados(solicitudActual)) {
            menuNavegacionActual.add(new MenuDto("📁 Formatos", "/formatos"));
        }
        model.addAttribute("menuNavegacionActual", menuNavegacionActual);

        EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario()).orElse(null);
        model.addAttribute("etapaActiva", etapaActiva);

        if (etapaActiva != null) {
            List<CronogramaBitacoras> cronograma = cronogramaService.obtenerOGenerarCronograma(etapaActiva);
            model.addAttribute("cronograma", cronograma);
            model.addAttribute("calendario",
                    cronogramaService.construirCalendario(etapaActiva.getFechaInicio(), etapaActiva.getFechaFin(), cronograma));
        } else {
            model.addAttribute("cronograma", Collections.emptyList());
            model.addAttribute("calendario", Collections.emptyList());
        }

        return "cronograma";
    }
}
