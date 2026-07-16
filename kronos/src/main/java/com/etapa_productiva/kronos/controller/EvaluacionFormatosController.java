package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.AprendizBitacoraDetalleDto;
import com.etapa_productiva.kronos.dto.AprendizBitacoraResumenDto;
import com.etapa_productiva.kronos.dto.AprendizPlaneacionResumenDto;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.entity.JuicioEvaluacion;
import com.etapa_productiva.kronos.entity.ResultadoEvaluacion;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.service.AuthService;
import com.etapa_productiva.kronos.service.EvaluacionFormatosService;
import com.etapa_productiva.kronos.service.ReporteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReporteService reporteService;

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

        return "instructor-planeacion";
    }

    /**
     * 📥 Exporta el listado del Formato 023 (estado de los 3 momentos por aprendiz) a Excel/PDF,
     * previa re-autenticación. POST /instructor/seguimiento/planeacion/exportar
     */
    @PostMapping("/instructor/seguimiento/planeacion/exportar")
    @ResponseBody
    public ResponseEntity<?> exportarPlaneacion(
            @RequestParam String formato,
            @RequestParam String contrasena,
            HttpSession session) throws Exception {

        LoginResponse usuario = validarAcceso(session);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }
        if (!esInstructorSeguimiento(usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "Solo el Instructor de Seguimiento puede exportar esta información."));
        }
        Usuario entidad = usuarioRepository.findById(usuario.getIdUsuario()).orElse(null);
        if (!authService.verificarContrasena(entidad, contrasena)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Contraseña incorrecta. Verifícala e inténtalo de nuevo."));
        }

        List<AprendizPlaneacionResumenDto> resumen = evaluacionFormatosService.listarResumenPlaneacion(usuario.getIdUsuario());
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = evaluacionFormatosService.exportarPlaneacionPdf(resumen);
            reporteService.registrar(usuario.getIdUsuario(), "PLANEACION_023", FormatoReporte.PDF, pdf);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=formato_023_" + fecha + ".pdf").body(pdf);
        }
        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = evaluacionFormatosService.exportarPlaneacionExcel(resumen);
            reporteService.registrar(usuario.getIdUsuario(), "PLANEACION_023", FormatoReporte.XLSX, excel);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=formato_023_" + fecha + ".xlsx").body(excel);
        }
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }

    /**
     * 📄 Plus: descarga el PDF de un solo momento (1, 2 o 3) apenas quede completo por ambos
     * lados, sin esperar a que los 3 estén listos para el Formato 023 completo.
     * GET /instructor/seguimiento/planeacion/{idEtapa}/momento/{numeroMomento}/pdf
     */
    @GetMapping("/instructor/seguimiento/planeacion/{idEtapa}/momento/{numeroMomento}/pdf")
    public ResponseEntity<?> descargarMomentoPdf(
            @PathVariable Long idEtapa,
            @PathVariable int numeroMomento,
            HttpSession session) {

        LoginResponse usuario = validarAcceso(session);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }
        if (!esInstructorSeguimiento(usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "Solo el Instructor de Seguimiento puede descargar este formato."));
        }

        try {
            byte[] pdf = evaluacionFormatosService.descargarMomentoPdf(usuario.getIdUsuario(), idEtapa, numeroMomento);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=formato_023_momento_" + numeroMomento + ".pdf")
                    .body(pdf);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PostMapping("/instructor/seguimiento/planeacion/momento")
    public String guardarObservacionMomento(
            @RequestParam Long idEtapa,
            @RequestParam int numeroMomento,
            @RequestParam(required = false) String observacion,
            @RequestParam(required = false) JuicioEvaluacion juicioEvaluacion,
            @RequestParam(required = false) String retroInstructorProceso,
            @RequestParam(required = false) String retroInstructorDesempeno,
            @RequestParam(required = false) String enlaceGrabacion,
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
            evaluacionFormatosService.guardarObservacionMomento(usuario.getIdUsuario(), idEtapa, numeroMomento,
                    observacion, juicioEvaluacion, retroInstructorProceso, retroInstructorDesempeno, enlaceGrabacion);
            redirectAttributes.addFlashAttribute("exito", "Observación guardada y notificada al aprendiz.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/seguimiento/planeacion";
    }

    @PostMapping(value = "/instructor/seguimiento/planeacion/firma", consumes = "multipart/form-data")
    public String guardarFirmaInstructor(
            @RequestParam Long idEtapa,
            @RequestParam("firma") MultipartFile firma,
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
            evaluacionFormatosService.guardarFirmaInstructor(usuario.getIdUsuario(), idEtapa, firma);
            redirectAttributes.addFlashAttribute("exito", "Firma guardada correctamente.");
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
