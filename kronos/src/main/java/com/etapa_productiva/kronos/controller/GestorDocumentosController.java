package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.DocumentoSolicitud;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.DocumentoSolicitudRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📄 Módulo "Validación de Documentos": bandeja del Gestor de Etapa con las solicitudes que
 * ya radicaron sus formatos iniciales y esperan que se les habiliten las plantillas, ordenadas
 * por aprendiz.
 */
@Controller
public class GestorDocumentosController {

    @Autowired
    private DocumentoSolicitudRepository documentoSolicitudRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @GetMapping("/gestor/documentos")
    public String verDocumentos(HttpSession session, Model model) {
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

        // 🗂️ Solicitudes que ya radicaron sus formatos iniciales y esperan que se les habiliten
        // las plantillas, ordenadas por nombre del aprendiz.
        List<SolicitudEtapaPractica> solicitudesParaHabilitarFormatos =
                solicitudRepository.findByEstadoAndPlantillasHabilitadas(EstadoSolicitud.FORMATOS_ENVIADOS, false).stream()
                        .sorted(Comparator.comparing((SolicitudEtapaPractica s) -> s.getAprendizFicha().getUsuario().getNombre(), String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(s -> s.getAprendizFicha().getUsuario().getApellido(), String.CASE_INSENSITIVE_ORDER))
                        .toList();
        model.addAttribute("solicitudesParaHabilitarFormatos", solicitudesParaHabilitarFormatos);

        // Asuntos de los archivos que cada aprendiz radicó, para mostrarlos en esa misma bandeja
        Map<Long, List<DocumentoSolicitud>> documentosPorSolicitud = new HashMap<>();
        for (SolicitudEtapaPractica s : solicitudesParaHabilitarFormatos) {
            documentosPorSolicitud.put(s.getIdSolicitud(),
                    documentoSolicitudRepository.findBySolicitudIdSolicitud(s.getIdSolicitud()));
        }
        model.addAttribute("documentosPorSolicitudParaHabilitar", documentosPorSolicitud);

        return "validacion-documentos";
    }
}
