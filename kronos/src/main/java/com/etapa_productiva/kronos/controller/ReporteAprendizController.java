package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.ReporteAprendizDto;
import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SeccionFormatoRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.service.AuthService;
import com.etapa_productiva.kronos.service.ReporteAprendizService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 📋 Módulo "Reporte Aprendiz" del rol Registro: buscador de solo lectura con los documentos
 * requisito ya diligenciados y APROBADOS de cada aprendiz, filtrable por nombre, apellido,
 * documento, tipo de documento, ficha y modalidad de contrato.
 */
@Controller
public class ReporteAprendizController {

    @Autowired
    private ReporteAprendizService reporteAprendizService;

    @Autowired
    private SeccionFormatoRepository seccionFormatoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/registro/reporte-aprendiz")
    public String verReporte(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @RequestParam(required = false) String ficha,
            @RequestParam(required = false) Long idSeccionFormato,
            HttpSession session, Model model) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        if (usuarioLogueado.getRoles() == null || !usuarioLogueado.getRoles().contains("REGISTRO")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        cargarFiltros(model, nombre, apellido, documento, tipoDocumento, ficha, idSeccionFormato);
        model.addAttribute("seccionesFormato", seccionFormatoRepository.findByEstadoTrue());

        boolean hayFiltro = tieneAlgunFiltro(nombre, apellido, documento, tipoDocumento, ficha, idSeccionFormato);
        model.addAttribute("resultados", hayFiltro
                ? reporteAprendizService.buscar(nombre, apellido, documento, tipoDocumento, ficha, idSeccionFormato)
                : Collections.emptyList());
        model.addAttribute("busquedaRealizada", hayFiltro);

        return "reporte-aprendiz";
    }

    /**
     * 📥 Exporta el reporte (aplicando los mismos filtros de la búsqueda activa) a Excel o PDF.
     * Re-autentica al usuario con su contraseña antes de generar el archivo, mismo patrón de
     * seguridad que Gestión Aprendices.
     */
    @PostMapping("/registro/reporte-aprendiz/exportar")
    @ResponseBody
    public ResponseEntity<?> exportarReporte(
            @RequestParam String formato,
            @RequestParam String contrasena,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @RequestParam(required = false) String ficha,
            @RequestParam(required = false) Long idSeccionFormato,
            HttpSession session) throws Exception {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }
        if (usuarioLogueado.getRoles() == null || !usuarioLogueado.getRoles().contains("REGISTRO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Solo el rol Registro puede exportar este reporte."));
        }

        Usuario usuario = usuarioRepository.findById(usuarioLogueado.getIdUsuario()).orElse(null);
        if (!authService.verificarContrasena(usuario, contrasena)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Contraseña incorrecta. Verifícala e inténtalo de nuevo."));
        }

        List<ReporteAprendizDto> resultados =
                reporteAprendizService.buscar(nombre, apellido, documento, tipoDocumento, ficha, idSeccionFormato);

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = reporteAprendizService.generarPdf(resultados);
            reporteService.registrar(usuarioLogueado.getIdUsuario(), "REPORTE_APRENDIZ", FormatoReporte.PDF, pdf);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=reporte_aprendiz_" + fecha + ".pdf")
                    .body(pdf);
        }

        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = reporteAprendizService.generarExcel(resultados);
            reporteService.registrar(usuarioLogueado.getIdUsuario(), "REPORTE_APRENDIZ", FormatoReporte.XLSX, excel);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=reporte_aprendiz_" + fecha + ".xlsx")
                    .body(excel);
        }

        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }

    static void cargarFiltros(Model model, String nombre, String apellido, String documento,
                              TipoDocumento tipoDocumento, String ficha, Long idSeccionFormato) {
        model.addAttribute("fNombre", nombre == null ? "" : nombre);
        model.addAttribute("fApellido", apellido == null ? "" : apellido);
        model.addAttribute("fDocumento", documento == null ? "" : documento);
        model.addAttribute("fTipoDocumento", tipoDocumento);
        model.addAttribute("fFicha", ficha == null ? "" : ficha);
        model.addAttribute("fIdSeccionFormato", idSeccionFormato);
        model.addAttribute("tiposDocumento", TipoDocumento.values());
    }

    static boolean tieneAlgunFiltro(String nombre, String apellido, String documento,
                                    TipoDocumento tipoDocumento, String ficha, Long idSeccionFormato) {
        return notBlank(nombre) || notBlank(apellido) || notBlank(documento) || notBlank(ficha)
                || tipoDocumento != null || idSeccionFormato != null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
