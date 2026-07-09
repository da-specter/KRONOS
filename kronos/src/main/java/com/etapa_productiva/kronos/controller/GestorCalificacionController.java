package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.DocumentoSolicitud;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.DocumentoSolicitudRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📄 Módulo "Calificar Documentos" del Gestor de Etapa: revisa los formatos/plantillas
 * firmadas que radicó el aprendiz y decide si aprueba modalidad y formatos. Si aprueba
 * ambos, la solicitud viaja a la bandeja del rol Registro para su propia validación.
 */
@Controller
public class GestorCalificacionController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private DocumentoSolicitudRepository documentoSolicitudRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private KronosWorkflowService workflowService;

    @GetMapping("/gestor/calificar-documentos")
    public String verCalificacionDocumentos(HttpSession session, Model model) {
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

        List<SolicitudEtapaPractica> solicitudesParaCalificar =
                solicitudRepository.findByEstado(EstadoSolicitud.FORMATOS_ENVIADOS).stream()
                        .sorted(Comparator.comparing((SolicitudEtapaPractica s) -> s.getAprendizFicha().getUsuario().getNombre(), String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(s -> s.getAprendizFicha().getUsuario().getApellido(), String.CASE_INSENSITIVE_ORDER))
                        .toList();
        model.addAttribute("solicitudesParaCalificar", solicitudesParaCalificar);

        Map<Long, List<DocumentoSolicitud>> documentosPorSolicitud = new HashMap<>();
        for (SolicitudEtapaPractica s : solicitudesParaCalificar) {
            documentosPorSolicitud.put(s.getIdSolicitud(),
                    documentoSolicitudRepository.findBySolicitudIdSolicitud(s.getIdSolicitud()));
        }
        model.addAttribute("documentosPorSolicitud", documentosPorSolicitud);

        return "gestor-calificar-documentos";
    }

    @PostMapping("/gestor/calificar-documentos/{idSolicitud}")
    public String calificarDocumentos(
            @PathVariable Long idSolicitud,
            @RequestParam(defaultValue = "false") boolean modalidadOk,
            @RequestParam(defaultValue = "false") boolean formatosOk,
            @RequestParam(required = false) String observacion,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("GESTOR_ETAPA")) {
            redirectAttributes.addFlashAttribute("error", "Solo un Gestor de Etapa puede calificar documentos.");
            return "redirect:/index";
        }

        try {
            workflowService.gestorCalificarDocumentos(idSolicitud, modalidadOk, formatosOk, observacion);
            redirectAttributes.addFlashAttribute("exito", modalidadOk && formatosOk
                    ? "Documentos calificados. La solicitud fue enviada al rol Registro."
                    : "Documentos rechazados. El aprendiz podrá corregir y reenviarlos.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/gestor/calificar-documentos";
    }
}
