package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.ReporteAprendizDto;
import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 📋 Módulo "Reporte" del Administrador: la misma evidencia de documentos requisito diligenciados
 * y APROBADOS que ve Registro, buscable por aprendiz, disponible también para el Administrador.
 */
@Controller
@RequestMapping("/admin")
public class AdminReporteController {

    @Autowired
    private ReporteAprendizService reporteAprendizService;

    @Autowired
    private SeccionFormatoRepository seccionFormatoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/reporte")
    public String verReporte(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @RequestParam(required = false) String ficha,
            @RequestParam(required = false) Long idSeccionFormato,
            HttpSession session, Model model) {

        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        model.addAttribute("usuario", admin);

        ReporteAprendizController.cargarFiltros(model, nombre, apellido, documento, tipoDocumento, ficha, idSeccionFormato);
        model.addAttribute("seccionesFormato", seccionFormatoRepository.findByEstadoTrue());

        boolean hayFiltro = ReporteAprendizController.tieneAlgunFiltro(nombre, apellido, documento, tipoDocumento, ficha, idSeccionFormato);
        model.addAttribute("resultados", hayFiltro
                ? reporteAprendizService.buscar(nombre, apellido, documento, tipoDocumento, ficha, idSeccionFormato)
                : Collections.emptyList());
        model.addAttribute("busquedaRealizada", hayFiltro);

        return "admin-reporte";
    }

    /**
     * 📥 Exporta el reporte (aplicando los mismos filtros de la búsqueda activa) a Excel o PDF.
     * Re-autentica al Administrador con su contraseña antes de generar el archivo.
     */
    @PostMapping("/reporte/exportar")
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

        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }

        Usuario usuario = usuarioRepository.findById(admin.getIdUsuario()).orElse(null);
        if (!authService.verificarContrasena(usuario, contrasena)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Contraseña incorrecta. Verifícala e inténtalo de nuevo."));
        }

        List<ReporteAprendizDto> resultados =
                reporteAprendizService.buscar(nombre, apellido, documento, tipoDocumento, ficha, idSeccionFormato);

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = reporteAprendizService.generarPdf(resultados);
            reporteService.registrar(admin.getIdUsuario(), "REPORTE_APRENDIZ", FormatoReporte.PDF, pdf);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=reporte_aprendiz_" + fecha + ".pdf")
                    .body(pdf);
        }

        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = reporteAprendizService.generarExcel(resultados);
            reporteService.registrar(admin.getIdUsuario(), "REPORTE_APRENDIZ", FormatoReporte.XLSX, excel);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=reporte_aprendiz_" + fecha + ".xlsx")
                    .body(excel);
        }

        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }
}
