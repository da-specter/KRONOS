package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.MenuDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.PlantillaFormato;
import com.etapa_productiva.kronos.entity.SeccionFormato;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.PlantillaFormatoRepository;
import com.etapa_productiva.kronos.repository.SeccionFormatoRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📁 Módulo "Formatos": catálogo institucional de plantillas agrupadas por
 * sección de tipo de contrato (Pasantía, Vínculo Laboral, Monitoría, Proyecto Productivo).
 * Visible para GESTOR_ETAPA siempre, y para APRENDIZ solo cuando su solicitud
 * ya fue aprobada en el primer filtro (ver IndexController.formatosDesbloqueados).
 */
@Controller
public class FormatosController {

    @Autowired
    private SeccionFormatoRepository seccionFormatoRepository;

    @Autowired
    private PlantillaFormatoRepository plantillaFormatoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @GetMapping("/formatos")
    public String verFormatos(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        boolean autorizado = roles != null && (roles.contains("APRENDIZ") || roles.contains("GESTOR_ETAPA"));
        if (!autorizado) {
            return "redirect:/index";
        }

        SolicitudEtapaPractica solicitudActual = null;
        if (roles.contains("APRENDIZ")) {
            solicitudActual = aprendizFichaRepository.findByUsuarioIdUsuario(usuarioLogueado.getIdUsuario())
                    .map(AprendizFicha::getIdAprendizFicha)
                    .flatMap(solicitudRepository::findByAprendizFichaIdAprendizFicha)
                    .orElse(null);

            if (!IndexController.formatosDesbloqueados(solicitudActual)) {
                redirectAttributes.addFlashAttribute("error",
                        "Aún no tienes el módulo de Formatos habilitado. Debes esperar a que el Gestor de Etapa apruebe el primer filtro de tu solicitud.");
                return "redirect:/index";
            }
        }
        model.addAttribute("solicitudActual", solicitudActual);

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        // Menú reactivo: refleja el mismo "📁 Formatos" condicional que usa /index (sin duplicarlo en la sesión).
        List<MenuDto> menuNavegacionActual = new ArrayList<>(
                usuarioLogueado.getMenuNavegacion() != null ? usuarioLogueado.getMenuNavegacion() : Collections.emptyList());
        if (roles.contains("APRENDIZ") && IndexController.formatosDesbloqueados(solicitudActual)) {
            menuNavegacionActual.add(new MenuDto("📁 Formatos", "/formatos"));
        }
        model.addAttribute("menuNavegacionActual", menuNavegacionActual);

        List<SeccionFormato> secciones = seccionFormatoRepository.findByEstadoTrue();
        model.addAttribute("secciones", secciones);

        Map<Long, List<PlantillaFormato>> plantillasPorSeccion = new HashMap<>();
        for (SeccionFormato seccion : secciones) {
            plantillasPorSeccion.put(seccion.getIdSeccionFormato(),
                    plantillaFormatoRepository.findWordExcelPorSeccion(seccion.getIdSeccionFormato()));
        }
        model.addAttribute("plantillasPorSeccion", plantillasPorSeccion);

        // Sección general "Etapa Práctica": documentos no atados a un tipo de contrato específico
        model.addAttribute("plantillasEtapaPractica", plantillaFormatoRepository.findWordExcelSinSeccion());

        return "formatos";
    }
}
