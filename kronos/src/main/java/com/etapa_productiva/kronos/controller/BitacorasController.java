package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.MenuDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.Bitacora;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.EvaluacionBitacora;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.BitacoraRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.EvaluacionBitacoraRepository;
import com.etapa_productiva.kronos.repository.FormatoPlaneacionRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.service.CronogramaService;
import com.etapa_productiva.kronos.service.SeguimientoEtapaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 📚 Módulo "Subir Bitácoras" del Aprendiz: dos secciones independientes,
 * Formato de Planeación y Bitácora, cada una con su propio asunto y archivo.
 */
@Controller
public class BitacorasController {

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private FormatoPlaneacionRepository formatoPlaneacionRepository;

    @Autowired
    private BitacoraRepository bitacoraRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private SeguimientoEtapaService seguimientoEtapaService;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private EvaluacionBitacoraRepository evaluacionBitacoraRepository;

    @Autowired
    private CronogramaService cronogramaService;

    @GetMapping("/aprendiz/bitacoras")
    public String verBitacoras(HttpSession session, Model model) {
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
            model.addAttribute("formatoPlaneacion",
                    formatoPlaneacionRepository.findByEtapaProductivaIdEtapa(etapaActiva.getIdEtapa()).orElse(null));

            List<Bitacora> bitacorasSubidas = bitacoraRepository.findByEtapaProductivaIdEtapaOrderByFechaEntregaDesc(etapaActiva.getIdEtapa());
            model.addAttribute("bitacorasSubidas", bitacorasSubidas);

            // Última evaluación de cada bitácora (si aún no hay ninguna, queda "en revisión")
            Map<Long, EvaluacionBitacora> ultimaEvaluacionPorBitacora = new HashMap<>();
            for (Bitacora bitacora : bitacorasSubidas) {
                evaluacionBitacoraRepository.findTopByBitacoraIdBitacoraOrderByFechaEvaluacionDesc(bitacora.getIdBitacora())
                        .ifPresent(evaluacion -> ultimaEvaluacionPorBitacora.put(bitacora.getIdBitacora(), evaluacion));
            }
            model.addAttribute("ultimaEvaluacionPorBitacora", ultimaEvaluacionPorBitacora);

            Set<Long> cronogramasYaSubidos = bitacorasSubidas.stream()
                    .map(b -> b.getCronogramaBitacora().getIdCronograma())
                    .collect(Collectors.toSet());

            List<CronogramaBitacoras> cronogramaPendiente = cronogramaService.obtenerOGenerarCronograma(etapaActiva)
                    .stream()
                    .filter(c -> !cronogramasYaSubidos.contains(c.getIdCronograma()))
                    .collect(Collectors.toList());
            model.addAttribute("cronogramaPendiente", cronogramaPendiente);
        } else {
            model.addAttribute("formatoPlaneacion", null);
            model.addAttribute("bitacorasSubidas", Collections.emptyList());
            model.addAttribute("ultimaEvaluacionPorBitacora", Collections.emptyMap());
            model.addAttribute("cronogramaPendiente", Collections.emptyList());
        }

        return "bitacoras";
    }

    @PostMapping(value = "/aprendiz/formato-planeacion", consumes = "multipart/form-data")
    public String subirFormatoPlaneacion(
            @RequestParam String asunto,
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            redirectAttributes.addFlashAttribute("error", "Solo un aprendiz puede subir el Formato de Planeación.");
            return "redirect:/index";
        }

        try {
            EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario())
                    .orElseThrow(() -> new IllegalStateException("No tienes una Etapa Productiva activa."));

            seguimientoEtapaService.subirFormatoPlaneacion(etapaActiva, asunto, archivo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/aprendiz/bitacoras";
    }

    @PostMapping(value = "/aprendiz/bitacora", consumes = "multipart/form-data")
    public String subirBitacora(
            @RequestParam Long idCronograma,
            @RequestParam String asunto,
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            redirectAttributes.addFlashAttribute("error", "Solo un aprendiz puede subir bitácoras.");
            return "redirect:/index";
        }

        try {
            EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario())
                    .orElseThrow(() -> new IllegalStateException("No tienes una Etapa Productiva activa."));

            seguimientoEtapaService.subirBitacora(etapaActiva, idCronograma, asunto, archivo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/aprendiz/bitacoras";
    }
}
