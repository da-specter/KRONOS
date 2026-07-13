package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.ResultadoImportacionVisitas;
import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.service.ImportacionVisitasSeguimientoService;
import com.etapa_productiva.kronos.service.ReporteService;
import com.etapa_productiva.kronos.service.VisitaSeguimientoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 📅 Módulo "Visitas de Seguimiento" del Instructor de Seguimiento, dividido en dos páginas:
 * "Agendar Citas" (formulario + aprendices sin visita vigente) y "Mi agenda de visitas"
 * (pasadas/pendientes/futuras, con cancelar/aplazar, evidencia y exportación).
 */
@Controller
public class InstructorVisitasController {

    @Autowired
    private VisitaSeguimientoService visitaSeguimientoService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private ImportacionVisitasSeguimientoService importacionVisitasSeguimientoService;

    /** ✏️ Agendar Citas: formulario de agendamiento + aprendices sin visita vigente. */
    @GetMapping("/instructor/visitas/agendar")
    public String verAgendarCitas(HttpSession session, Model model) {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuario.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario()));
        model.addAttribute("aprendicesAsignados", visitaSeguimientoService.listarAprendicesParaAgendar(usuario.getIdUsuario()));
        model.addAttribute("aprendicesSinVisita", visitaSeguimientoService.listarAprendicesSinVisitaVigente(usuario.getIdUsuario()));

        return "agendar-citas";
    }

    @PostMapping("/instructor/visitas/agendar")
    public String agendarVisita(
            @RequestParam Long idEtapa,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam String modalidad,
            @RequestParam(required = false) String novedad,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuario.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            return "redirect:/index";
        }

        try {
            visitaSeguimientoService.agendarVisita(usuario.getIdUsuario(), idEtapa, LocalDateTime.of(fecha, hora), modalidad, novedad);
            redirectAttributes.addFlashAttribute("exito", "Visita agendada correctamente. El aprendiz ya fue notificado. "
                    + "Si es la primera visita de este aprendiz, KRONOS ya agendó automáticamente las otras 2.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/instructor/visitas/agendar";
    }

    /**
     * 📥 Importa masivamente el Excel institucional de "Visitas de Seguimiento": crea el
     * usuario aprendiz, la ficha, la empresa, la Etapa Productiva (asignada a quien importa
     * como Instructor de Seguimiento) y la visita ya agendada, todo por fila y sin margen de
     * error entre filas (una fila con datos inválidos no afecta a las demás).
     * POST /instructor/visitas/importar (multipart/form-data)
     */
    @PostMapping(value = "/instructor/visitas/importar", consumes = "multipart/form-data")
    public String importarVisitas(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fichaFechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fichaFechaFin,
            @RequestParam(required = false) String tipoContratoRespaldo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuario.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            return "redirect:/index";
        }

        try {
            ResultadoImportacionVisitas r = importacionVisitasSeguimientoService.importar(
                    archivo, usuario.getIdUsuario(), fichaFechaInicio, fichaFechaFin, tipoContratoRespaldo);

            StringBuilder resumen = new StringBuilder("Importación completada: ")
                    .append(r.filas()).append(" filas procesadas. ")
                    .append("Visitas agendadas: ").append(r.visitasCreadas()).append(", ")
                    .append("aprendices nuevos: ").append(r.usuariosCreados()).append(", ")
                    .append("etapas productivas creadas: ").append(r.etapasCreadas()).append(", ")
                    .append("empresas nuevas: ").append(r.empresasCreadas()).append(", ")
                    .append("fichas nuevas: ").append(r.fichasCreadas()).append(", ")
                    .append("programas nuevos: ").append(r.programasCreados()).append(", ")
                    .append("tipos de contrato nuevos: ").append(r.tiposContratoCreados()).append(".");
            if (r.omitidas() > 0) {
                resumen.append(" ⏭ ").append(r.omitidas()).append(" fila(s) omitida(s) (el aprendiz ya tenía Etapa Productiva).");
            }
            if (!r.errores().isEmpty()) {
                resumen.append(" ⚠️ ").append(r.errores().size()).append(" fila(s) con error: ")
                        .append(String.join(" | ", r.errores().subList(0, Math.min(5, r.errores().size()))));
            }
            redirectAttributes.addFlashAttribute("exito", resumen.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/instructor/visitas/agendar";
    }

    /** 📋 Mi agenda de visitas: pasadas / pendientes (hoy) / futuras, con acciones y exportación. */
    @GetMapping("/instructor/visitas")
    public String verAgenda(HttpSession session, Model model) {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuario.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario()));
        model.addAttribute("agenda", visitaSeguimientoService.listarAgendaInstructor(usuario.getIdUsuario()));

        return "mi-agenda-visitas";
    }

    /**
     * ✋ Cancela o aplaza una visita (mientras siga futura o pendiente de hoy), dejando
     * una novedad obligatoria con el motivo. Notifica al aprendiz del cambio.
     * POST /instructor/visitas/gestionar
     */
    @PostMapping("/instructor/visitas/gestionar")
    public String gestionarVisita(
            @RequestParam Long idVisita,
            @RequestParam String accion,
            @RequestParam String novedad,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuario.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            return "redirect:/index";
        }

        try {
            visitaSeguimientoService.cambiarEstadoVisita(usuario.getIdUsuario(), idVisita, accion, novedad);
            String verbo = "CANCELAR".equalsIgnoreCase(accion) ? "cancelada" : "aplazada";
            redirectAttributes.addFlashAttribute("exito", "La visita quedó " + verbo + ". El aprendiz ya fue notificado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/instructor/visitas";
    }

    /**
     * 📎 Adjunta (o reemplaza) el archivo de evidencia (Word/PDF/Excel/imagen) de una
     * visita ya REALIZADA. Solo el instructor que la agendó puede subirla.
     * POST /instructor/visitas/evidencia (multipart/form-data)
     */
    @PostMapping(value = "/instructor/visitas/evidencia", consumes = "multipart/form-data")
    public String subirEvidencia(
            @RequestParam Long idVisita,
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuario.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            return "redirect:/index";
        }

        try {
            visitaSeguimientoService.subirEvidencia(usuario.getIdUsuario(), idVisita, archivo);
            redirectAttributes.addFlashAttribute("exito", "Evidencia cargada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/instructor/visitas";
    }

    /**
     * 📥 Exporta las visitas "pasadas" del instructor (histórico: vencidas, canceladas o
     * aplazadas) a Excel o PDF. Registra el archivo en el historial de reportes.
     * GET /instructor/visitas/exportar?formato=excel|pdf
     */
    @GetMapping("/instructor/visitas/exportar")
    @ResponseBody
    public ResponseEntity<?> exportarPasadas(@RequestParam String formato, HttpSession session) throws Exception {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }
        List<String> roles = usuario.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            return ResponseEntity.status(403).body(Map.of("mensaje", "Solo el Instructor de Seguimiento puede exportar esta información."));
        }

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = visitaSeguimientoService.exportarPasadasPdf(usuario.getIdUsuario());
            reporteService.registrar(usuario.getIdUsuario(), "INSTRUCTOR_VISITAS", FormatoReporte.PDF, pdf);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=visitas_pasadas_" + fecha + ".pdf").body(pdf);
        }
        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = visitaSeguimientoService.exportarPasadasExcel(usuario.getIdUsuario());
            reporteService.registrar(usuario.getIdUsuario(), "INSTRUCTOR_VISITAS", FormatoReporte.XLSX, excel);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=visitas_pasadas_" + fecha + ".xlsx").body(excel);
        }
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }
}
