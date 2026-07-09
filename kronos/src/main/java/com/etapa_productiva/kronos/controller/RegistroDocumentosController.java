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
 * 📄 Módulo "Validación de Documentos" del rol REGISTRO: bandeja con las solicitudes que el
 * Gestor de Etapa ya calificó y envió, para que Registro las valide antes de proceder a
 * registrar la Etapa Productiva.
 */
@Controller
public class RegistroDocumentosController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private DocumentoSolicitudRepository documentoSolicitudRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private KronosWorkflowService workflowService;

    @GetMapping("/registro/documentos")
    public String verValidacionDocumentos(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("REGISTRO")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        List<SolicitudEtapaPractica> solicitudesParaValidar =
                solicitudRepository.findByEstado(EstadoSolicitud.EN_VALIDACION_REGISTRO).stream()
                        .sorted(Comparator.comparing((SolicitudEtapaPractica s) -> s.getAprendizFicha().getUsuario().getNombre(), String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(s -> s.getAprendizFicha().getUsuario().getApellido(), String.CASE_INSENSITIVE_ORDER))
                        .toList();
        model.addAttribute("solicitudesParaValidar", solicitudesParaValidar);

        Map<Long, List<DocumentoSolicitud>> documentosPorSolicitud = new HashMap<>();
        for (SolicitudEtapaPractica s : solicitudesParaValidar) {
            documentosPorSolicitud.put(s.getIdSolicitud(),
                    documentoSolicitudRepository.findBySolicitudIdSolicitud(s.getIdSolicitud()));
        }
        model.addAttribute("documentosPorSolicitud", documentosPorSolicitud);

        return "registro-documentos";
    }

    @PostMapping("/registro/documentos/{idSolicitud}")
    public String validarDocumentos(
            @PathVariable Long idSolicitud,
            @RequestParam boolean aprobado,
            @RequestParam(required = false) String observacion,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("REGISTRO")) {
            redirectAttributes.addFlashAttribute("error", "Solo el rol Registro puede validar documentos.");
            return "redirect:/index";
        }

        try {
            workflowService.registroValidarDocumentos(idSolicitud, aprobado, observacion);
            redirectAttributes.addFlashAttribute("exito", aprobado
                    ? "Documentos validados. Ya puedes registrar la Etapa Productiva."
                    : "La solicitud fue devuelta al Gestor de Etapa con tu observación.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/registro/documentos";
    }
}
