package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.TecnicoAprendizDto;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.service.InstructorTecnicoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
