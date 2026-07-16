package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.InstructorAprendizDto;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.service.AuthService;
import com.etapa_productiva.kronos.service.GestionFichasService;
import com.etapa_productiva.kronos.service.InstructorSeguimientoService;
import com.etapa_productiva.kronos.service.ReporteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 👨‍🏫 Módulo "Mis Aprendices" del Instructor de Seguimiento: lista de sus aprendices y su
 * etapa, con exportación protegida por re-autenticación (guardando el reporte en REPORTE)
 * e importación de aprendices.
 */
@Controller
public class InstructorSeguimientoController {

    @Autowired
    private InstructorSeguimientoService instructorSeguimientoService;

    @Autowired
    private GestionFichasService gestionFichasService;

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @GetMapping("/instructor/seguimiento")
    public String verMisAprendices(HttpSession session, Model model) {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("INSTRUCTOR_SEGUIMIENTO")) {
            return "redirect:/index";
        }

        List<InstructorAprendizDto> aprendices = instructorSeguimientoService.listarAprendices(usuario.getIdUsuario());
        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario()));
        model.addAttribute("aprendices", aprendices);
        model.addAttribute("dashInstructor", instructorSeguimientoService.calcularDashboard(aprendices));
        // 📅 Filtro "por mes": meses en los que realmente tiene una asignación vigente,
        // más reciente primero (ver FechaUtil / columna "Asignado Desde").
        model.addAttribute("mesesAsignacion", aprendices.stream()
                .map(InstructorAprendizDto::getMesAsignacion)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(java.util.Comparator.reverseOrder())
                .map(clave -> new com.etapa_productiva.kronos.dto.MesOpcion(clave, com.etapa_productiva.kronos.util.FechaUtil.etiquetaMes(clave)))
                .toList());

        return "mis-aprendices";
    }

    /**
     * 📥 Exporta la lista de aprendices del instructor (Excel/PDF), previa re-autenticación.
     * Registra el reporte en REPORTE (APRENDIZ_EXCEL o EJECUTIVO).
     * POST /instructor/seguimiento/exportar
     */
    @PostMapping("/instructor/seguimiento/exportar")
    @ResponseBody
    public ResponseEntity<?> exportar(
            @RequestParam String formato,
            @RequestParam String contrasena,
            HttpSession session) throws Exception {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("INSTRUCTOR_SEGUIMIENTO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "Solo el Instructor de Seguimiento puede exportar esta información."));
        }
        Usuario entidad = usuarioRepository.findById(usuario.getIdUsuario()).orElse(null);
        if (!authService.verificarContrasena(entidad, contrasena)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Contraseña incorrecta. Verifícala e inténtalo de nuevo."));
        }

        List<InstructorAprendizDto> aprendices = instructorSeguimientoService.listarAprendices(usuario.getIdUsuario());
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = instructorSeguimientoService.exportarPdf(aprendices);
            reporteService.registrar(usuario.getIdUsuario(), "INSTRUCTOR_APRENDICES", FormatoReporte.PDF, pdf);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=mis_aprendices_" + fecha + ".pdf").body(pdf);
        }
        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = instructorSeguimientoService.exportarExcel(aprendices);
            reporteService.registrar(usuario.getIdUsuario(), "INSTRUCTOR_APRENDICES", FormatoReporte.XLSX, excel);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=mis_aprendices_" + fecha + ".xlsx").body(excel);
        }
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }

    /**
     * 📋 Descarga la plantilla Excel en blanco para diligenciar la importación de
     * aprendices+ficha. GET /instructor/seguimiento/plantilla
     */
    @GetMapping("/instructor/seguimiento/plantilla")
    public ResponseEntity<byte[]> descargarPlantillaImportacion(HttpSession session) throws IOException {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/auth/login").build();
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("INSTRUCTOR_SEGUIMIENTO")) {
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/index").build();
        }

        byte[] excel = gestionFichasService.generarPlantillaImportacion();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("Content-Disposition", "attachment; filename=plantilla_importacion_aprendices_ficha.xlsx")
                .body(excel);
    }

    /**
     * 📤 Importa un Excel de aprendices+ficha (reutiliza el importador de Gestión de Fichas).
     * POST /instructor/seguimiento/importar (multipart/form-data)
     */
    @PostMapping(value = "/instructor/seguimiento/importar", consumes = "multipart/form-data")
    public String importar(
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("INSTRUCTOR_SEGUIMIENTO")) {
            return "redirect:/index";
        }

        try {
            GestionFichasService.ResultadoImportacion r = gestionFichasService.importar(archivo);
            StringBuilder resumen = new StringBuilder("Importación completada: ")
                    .append(r.filas()).append(" filas procesadas. Fichas nuevas: ").append(r.fichasCreadas())
                    .append(", programas nuevos: ").append(r.programasCreados())
                    .append(", aprendices nuevos: ").append(r.aprendicesCreados())
                    .append(", matrículas nuevas: ").append(r.matriculasCreadas()).append(".");
            if (!r.errores().isEmpty()) {
                resumen.append(" ⚠️ ").append(r.errores().size()).append(" fila(s) con error: ")
                        .append(String.join(" | ", r.errores().subList(0, Math.min(5, r.errores().size()))));
            }
            redirectAttributes.addFlashAttribute("exito", resumen.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/seguimiento";
    }
}
