package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.TecnicoAprendizDto;
import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.service.AuthService;
import com.etapa_productiva.kronos.service.InstructorTecnicoService;
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
 * 🛠️ Módulo "Mis Fichas" del Instructor Técnico (líder de ficha): consulta de sus fichas
 * y de los aprendices matriculados en ellas con su etapa práctica, buscador multi-criterio,
 * y la posibilidad de añadir aprendices a sus fichas (manual o por importación Excel).
 */
@Controller
public class InstructorTecnicoController {

    @Autowired
    private InstructorTecnicoService instructorTecnicoService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/instructor/tecnico")
    public String verMisFichas(HttpSession session, Model model) {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("INSTRUCTOR_TECNICO")) {
            return "redirect:/index";
        }

        List<TecnicoAprendizDto> aprendices = instructorTecnicoService.listarAprendices(usuario.getIdUsuario());

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario()));
        model.addAttribute("fichasTecnico", instructorTecnicoService.listarFichas(usuario.getIdUsuario()));
        model.addAttribute("aprendices", aprendices);
        model.addAttribute("dashTecnico", instructorTecnicoService.calcularDashboard(aprendices));

        // Opciones de los filtros del buscador
        model.addAttribute("tiposDocumento", TipoDocumento.values());
        model.addAttribute("modalidadesContrato",
                aprendices.stream().map(TecnicoAprendizDto::getModalidadContrato)
                        .filter(m -> !"—".equals(m)).distinct().sorted().toList());
        model.addAttribute("modalidades",
                aprendices.stream().map(TecnicoAprendizDto::getModalidad)
                        .filter(m -> !"—".equals(m)).distinct().sorted().toList());

        return "instructor-tecnico";
    }

    /**
     * 📥 Exporta los aprendices de las fichas del instructor a Excel o PDF, previa
     * re-autenticación (se vuelve a pedir la contraseña, igual que en Gestión Aprendices).
     * POST /instructor/tecnico/exportar
     */
    @PostMapping("/instructor/tecnico/exportar")
    @ResponseBody
    public ResponseEntity<?> exportarAprendices(
            @RequestParam String formato,
            @RequestParam String contrasena,
            HttpSession session) throws Exception {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("INSTRUCTOR_TECNICO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Solo el Instructor Técnico puede exportar esta información."));
        }

        // 🔐 Re-autenticación: la contraseña ingresada debe ser la del usuario en sesión
        Usuario usuario = usuarioRepository.findById(usuarioLogueado.getIdUsuario()).orElse(null);
        if (!authService.verificarContrasena(usuario, contrasena)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Contraseña incorrecta. Verifícala e inténtalo de nuevo."));
        }

        List<TecnicoAprendizDto> aprendices = instructorTecnicoService.listarAprendices(usuarioLogueado.getIdUsuario());
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = instructorTecnicoService.generarPdf(aprendices);
            reporteService.registrar(usuarioLogueado.getIdUsuario(), "APRENDICES_TECNICO", FormatoReporte.PDF, pdf);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=mis_aprendices_tecnico_" + fecha + ".pdf")
                    .body(pdf);
        }

        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = instructorTecnicoService.generarExcel(aprendices);
            reporteService.registrar(usuarioLogueado.getIdUsuario(), "APRENDICES_TECNICO", FormatoReporte.XLSX, excel);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=mis_aprendices_tecnico_" + fecha + ".xlsx")
                    .body(excel);
        }

        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }

    /**
     * ➕ Añade (matricula) un aprendiz en una de las fichas del instructor. Si el documento
     * no existe en el sistema, crea el Usuario con rol APRENDIZ.
     * POST /instructor/tecnico/agregar
     */
    @PostMapping("/instructor/tecnico/agregar")
    public String agregarAprendiz(
            @RequestParam Long idFicha,
            @RequestParam String tipoDocumento,
            @RequestParam String documento,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String correo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("INSTRUCTOR_TECNICO")) {
            return "redirect:/index";
        }

        try {
            String mensaje = instructorTecnicoService.agregarAprendiz(
                    usuario.getIdUsuario(), idFicha, tipoDocumento, documento, nombre, apellido, telefono, correo);
            redirectAttributes.addFlashAttribute("exito", mensaje);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/tecnico";
    }

    /**
     * 📋 Descarga la plantilla Excel en blanco para diligenciar la importación de
     * aprendices a una ficha. GET /instructor/tecnico/plantilla
     */
    @GetMapping("/instructor/tecnico/plantilla")
    public ResponseEntity<byte[]> descargarPlantillaImportacion(HttpSession session) throws IOException {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/auth/login").build();
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("INSTRUCTOR_TECNICO")) {
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/index").build();
        }

        byte[] excel = instructorTecnicoService.generarPlantillaImportacion();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("Content-Disposition", "attachment; filename=plantilla_importacion_aprendices.xlsx")
                .body(excel);
    }

    /**
     * 📤 Importa un Excel de aprendices y los matricula en la ficha seleccionada del instructor.
     * POST /instructor/tecnico/importar (multipart/form-data)
     */
    @PostMapping(value = "/instructor/tecnico/importar", consumes = "multipart/form-data")
    public String importarAprendices(
            @RequestParam Long idFicha,
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("INSTRUCTOR_TECNICO")) {
            return "redirect:/index";
        }

        try {
            InstructorTecnicoService.ResultadoImportacionTecnico r =
                    instructorTecnicoService.importarAprendices(usuario.getIdUsuario(), idFicha, archivo);
            StringBuilder resumen = new StringBuilder("Importación completada: ")
                    .append(r.filas()).append(" filas procesadas. Aprendices nuevos: ").append(r.aprendicesCreados())
                    .append(", matrículas nuevas: ").append(r.matriculasCreadas()).append(".");
            if (!r.errores().isEmpty()) {
                resumen.append(" ⚠️ ").append(r.errores().size()).append(" fila(s) con error: ")
                        .append(String.join(" | ", r.errores().subList(0, Math.min(5, r.errores().size()))));
            }
            redirectAttributes.addFlashAttribute("exito", resumen.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/tecnico";
    }
}
