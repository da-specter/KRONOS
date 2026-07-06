package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.PlantillaFormato;
import com.etapa_productiva.kronos.entity.SeccionFormato;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
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

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

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
        EtapaProductiva etapaActiva = null;
        if (roles.contains("APRENDIZ")) {
            solicitudActual = aprendizFichaRepository.findByUsuarioIdUsuario(usuarioLogueado.getIdUsuario())
                    .map(AprendizFicha::getIdAprendizFicha)
                    .flatMap(solicitudRepository::findByAprendizFichaIdAprendizFicha)
                    .orElse(null);
            etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario()).orElse(null);

            if (!IndexController.formatosDesbloqueados(solicitudActual) || IndexController.etapaCertificando(etapaActiva)) {
                redirectAttributes.addFlashAttribute("error",
                        "El módulo de Formatos no está disponible para tu proceso en este momento.");
                return "redirect:/index";
            }
        }
        model.addAttribute("solicitudActual", solicitudActual);

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        // Menú reactivo: refleja el mismo "📁 Formatos" condicional que usa /index (sin duplicarlo en la sesión).
        model.addAttribute("menuNavegacionActual", roles.contains("APRENDIZ")
                ? IndexController.menuAprendizReactivo(usuarioLogueado, solicitudActual, etapaActiva)
                : usuarioLogueado.getMenuNavegacion());

        // El Gestor de Etapa ve el catálogo completo (las 4 modalidades de contrato);
        // el Aprendiz solo ve la tarjeta de la modalidad que escogió en su solicitud.
        List<SeccionFormato> todasLasSecciones = seccionFormatoRepository.findByEstadoTrue();
        List<SeccionFormato> secciones = todasLasSecciones;
        if (solicitudActual != null) {
            Long idSeccionElegida = solicitudActual.getSeccionFormato().getIdSeccionFormato();
            secciones = todasLasSecciones.stream()
                    .filter(s -> s.getIdSeccionFormato().equals(idSeccionElegida))
                    .toList();
        }
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
