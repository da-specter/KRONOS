package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.AprendizGestionDto;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.service.AuthService;
import com.etapa_productiva.kronos.service.GestionAprendicesService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 🎓 Módulo "Gestión Aprendices" (sub-ítem del módulo Gestión Etapa del Gestor de Etapa):
 * consulta/búsqueda de la información consolidada de los aprendices y exportación a
 * Excel/PDF protegida con re-autenticación (se vuelve a pedir la contraseña).
 */
@Controller
public class GestionAprendicesController {

    @Autowired
    private GestionAprendicesService gestionAprendicesService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/gestor/aprendices")
    public String verAprendices(HttpSession session, Model model) {
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
        model.addAttribute("aprendices", gestionAprendicesService.listarAprendices());

        return "gestion-aprendices";
    }

    /**
     * 📎 Sube (o reemplaza) el soporte de la ARL de un aprendiz desde el cuadro ARL de la tabla.
     * El archivo queda guardado como DocumentoRequisito de su Etapa Productiva.
     * POST /gestor/aprendices/arl (multipart/form-data)
     */
    @PostMapping(value = "/gestor/aprendices/arl", consumes = "multipart/form-data")
    public String subirArl(
            @RequestParam Long idEtapa,
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("GESTOR_ETAPA")) {
            return "redirect:/index";
        }

        try {
            gestionAprendicesService.subirArl(idEtapa, archivo);
            redirectAttributes.addFlashAttribute("exito", "ARL cargada correctamente: quedó guardada y aprobada como documento requisito de la etapa.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/gestor/aprendices";
    }

    /**
     * 📥 Exporta el listado (aplicando el filtro de búsqueda activo) a Excel o PDF.
     * Antes de generar el archivo se re-autentica al Gestor de Etapa: si la contraseña
     * no coincide con la de su cuenta, se responde 401 y no se descarga nada.
     */
    @PostMapping("/gestor/aprendices/exportar")
    @ResponseBody
    public ResponseEntity<?> exportarAprendices(
            @RequestParam String formato,
            @RequestParam String contrasena,
            @RequestParam(required = false) String filtro,
            HttpSession session) throws Exception {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("GESTOR_ETAPA")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Solo el Gestor de Etapa puede exportar esta información."));
        }

        // 🔐 Re-autenticación: la contraseña ingresada debe ser la del usuario en sesión
        Usuario usuario = usuarioRepository.findById(usuarioLogueado.getIdUsuario()).orElse(null);
        if (!authService.verificarContrasena(usuario, contrasena)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Contraseña incorrecta. Verifícala e inténtalo de nuevo."));
        }

        List<AprendizGestionDto> aprendices =
                gestionAprendicesService.filtrar(gestionAprendicesService.listarAprendices(), filtro);

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = gestionAprendicesService.generarPdf(aprendices);
            reporteService.registrar(usuarioLogueado.getIdUsuario(), "APRENDICES", FormatoReporte.PDF, pdf);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=aprendices_etapa_productiva_" + fecha + ".pdf")
                    .body(pdf);
        }

        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = gestionAprendicesService.generarExcel(aprendices);
            reporteService.registrar(usuarioLogueado.getIdUsuario(), "APRENDICES", FormatoReporte.XLSX, excel);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=aprendices_etapa_productiva_" + fecha + ".xlsx")
                    .body(excel);
        }

        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }
}
