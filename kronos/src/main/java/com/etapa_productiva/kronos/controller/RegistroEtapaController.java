package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.service.KronosWorkflowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * 🏢 Módulo "Registro Etapa Productiva" del Gestor de Etapa: registra formalmente la
 * Etapa Productiva (empresa, tipo de contrato, fechas, jefe inmediato) de una solicitud
 * que ya pasó por los filtros y formatos, dejándola en APROBADO_EN_ETAPA.
 */
@Controller
public class RegistroEtapaController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private KronosWorkflowService workflowService;

    @GetMapping("/gestor/registro-etapa")
    public String verRegistroEtapa(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("GESTOR_ETAPA")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("solicitudesParaRegistrar", solicitudRepository.findByEstado(EstadoSolicitud.FORMATOS_ENVIADOS));

        return "registro-etapa";
    }

    @PostMapping("/gestor/registro-etapa/{idSolicitud}")
    public String registrarEtapa(
            @PathVariable Long idSolicitud,
            @RequestParam Long idAprendizFicha,
            @RequestParam(defaultValue = "false") boolean modalidadOk,
            @RequestParam(defaultValue = "false") boolean formatosOk,
            @RequestParam String nit,
            @RequestParam String nombreEmpresa,
            @RequestParam String direccionEmpresa,
            @RequestParam(required = false) String telefonoEmpresa,
            @RequestParam(required = false) String correoEmpresa,
            @RequestParam String nombreMunicipio,
            @RequestParam String nombreDepartamento,
            @RequestParam String nombreTipoContrato,
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin,
            @RequestParam String nombreJefeInmediato,
            @RequestParam String correoJefeInmediato,
            @RequestParam String telefonoJefeInmediato,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("GESTOR_ETAPA")) {
            redirectAttributes.addFlashAttribute("error", "Solo un Gestor de Etapa puede registrar la Etapa Productiva.");
            return "redirect:/index";
        }

        try {
            workflowService.registrarEtapaProductiva(
                    idSolicitud, idAprendizFicha, modalidadOk, formatosOk,
                    nit, nombreEmpresa, direccionEmpresa, telefonoEmpresa, correoEmpresa,
                    nombreMunicipio, nombreDepartamento, nombreTipoContrato,
                    fechaInicio, fechaFin, nombreJefeInmediato, correoJefeInmediato, telefonoJefeInmediato);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/gestor/registro-etapa";
    }
}
