package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.AprendizBitacoraDetalleDto;
import com.etapa_productiva.kronos.dto.AprendizBitacoraResumenDto;
import com.etapa_productiva.kronos.dto.AprendizPlaneacionResumenDto;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.ResultadoEvaluacion;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.service.EvaluacionFormatosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 🛠️ Módulo "Evaluación de Formatos" del Instructor de Seguimiento: revisión y calificación
 * de las Bitácoras y el Formato de Planeación (023) de los aprendices que tiene asignados.
 */
@Controller
public class EvaluacionFormatosController {

    @Autowired
    private EvaluacionFormatosService evaluacionFormatosService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    // ─────────────────────────────────── Bitácoras ───────────────────────────────────

    @GetMapping("/instructor/seguimiento/bitacoras")
    public String verBitacoras(HttpSession session, Model model) {
        LoginResponse usuario = validarAcceso(session);
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (!esInstructorSeguimiento(usuario)) {
            return "redirect:/index";
        }

        List<AprendizBitacoraResumenDto> resumen = evaluacionFormatosService.listarResumenBitacoras(usuario.getIdUsuario());

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario()));
        model.addAttribute("resumenBitacoras", resumen);
        model.addAttribute("dashBitacoras", evaluacionFormatosService.calcularDashboardBitacoras(resumen));

        return "instructor-bitacoras";
    }

    @GetMapping("/instructor/seguimiento/bitacoras/{idEtapa}")
    public String verBitacorasAprendiz(@PathVariable Long idEtapa, HttpSession session, Model model,
                                       RedirectAttributes redirectAttributes) {
        LoginResponse usuario = validarAcceso(session);
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (!esInstructorSeguimiento(usuario)) {
            return "redirect:/index";
        }

        AprendizBitacoraDetalleDto detalle;
        try {
            detalle = evaluacionFormatosService.listarBitacorasDeEtapa(usuario.getIdUsuario(), idEtapa);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/instructor/seguimiento/bitacoras";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario()));
        model.addAttribute("detalle", detalle);
        model.addAttribute("resultados", ResultadoEvaluacion.values());

        return "instructor-bitacoras-detalle";
    }

    @PostMapping("/instructor/seguimiento/bitacoras/evaluar")
    public String evaluarBitacora(
            @RequestParam Long idBitacora,
            @RequestParam Long idEtapa,
            @RequestParam ResultadoEvaluacion resultado,
            @RequestParam(required = false) String observaciones,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = validarAcceso(session);
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (!esInstructorSeguimiento(usuario)) {
            return "redirect:/index";
        }

        try {
            evaluacionFormatosService.evaluarBitacora(usuario.getIdUsuario(), idBitacora, resultado, observaciones);
            redirectAttributes.addFlashAttribute("exito", "Evaluación registrada y notificada al aprendiz.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/seguimiento/bitacoras/" + idEtapa;
    }

    // ─────────────────────────── Formato de Planeación (023) ───────────────────────────

    @GetMapping("/instructor/seguimiento/planeacion")
    public String verPlaneacion(HttpSession session, Model model) {
        LoginResponse usuario = validarAcceso(session);
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (!esInstructorSeguimiento(usuario)) {
            return "redirect:/index";
        }

        List<AprendizPlaneacionResumenDto> resumen = evaluacionFormatosService.listarResumenPlaneacion(usuario.getIdUsuario());

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario()));
        model.addAttribute("resumenPlaneacion", resumen);
        model.addAttribute("resultados", ResultadoEvaluacion.values());

        return "instructor-planeacion";
    }

    @PostMapping("/instructor/seguimiento/planeacion/evaluar")
    public String evaluarPlaneacion(
            @RequestParam Long idFormatoPlaneacion,
            @RequestParam ResultadoEvaluacion resultado,
            @RequestParam(required = false) String observaciones,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = validarAcceso(session);
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (!esInstructorSeguimiento(usuario)) {
            return "redirect:/index";
        }

        try {
            evaluacionFormatosService.evaluarPlaneacion(usuario.getIdUsuario(), idFormatoPlaneacion, resultado, observaciones);
            redirectAttributes.addFlashAttribute("exito", "Evaluación registrada y notificada al aprendiz.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/seguimiento/planeacion";
    }

    // ─────────────────────────────────── Helpers ───────────────────────────────────

    private LoginResponse validarAcceso(HttpSession session) {
        return (LoginResponse) session.getAttribute("usuarioSesion");
    }

    private boolean esInstructorSeguimiento(LoginResponse usuario) {
        return usuario.getRoles() != null && usuario.getRoles().contains("INSTRUCTOR_SEGUIMIENTO");
    }
}
