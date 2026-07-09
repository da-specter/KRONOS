package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AccionAuditoria;
import com.etapa_productiva.kronos.repository.AuditoriaRepository;
import com.etapa_productiva.kronos.repository.JobEjecucionRepository;
import com.etapa_productiva.kronos.service.AuditoriaService;
import com.etapa_productiva.kronos.service.BitacoraAlertaService;
import com.etapa_productiva.kronos.service.ConfiguracionGlobalService;
import com.etapa_productiva.kronos.service.VisitaAlertaService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * ⚙️ Módulo "Sistema y Automatización" del Administrador:
 * Monitoreo de Jobs (historial del proceso automático de la 1:00 a.m.),
 * Auditoría (logs de todas las acciones del sistema) y Configuración Global.
 */
@Controller
@RequestMapping("/admin")
public class AdminSistemaController {

    @Autowired private JobEjecucionRepository jobEjecucionRepository;
    @Autowired private AuditoriaRepository auditoriaRepository;
    @Autowired private ConfiguracionGlobalService configuracionGlobalService;
    @Autowired private AuditoriaService auditoriaService;
    @Autowired private VisitaAlertaService visitaAlertaService;
    @Autowired private BitacoraAlertaService bitacoraAlertaService;

    // ══════════════════════════ MONITOREO DE JOBS ══════════════════════════

    @GetMapping("/jobs")
    public String verJobs(HttpSession session, Model model) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        model.addAttribute("usuario", admin);
        model.addAttribute("ejecuciones", jobEjecucionRepository.findTop100ByOrderByFechaInicioDesc());
        model.addAttribute("nombreJob", "Alertas automáticas (visitas 1:00 a.m. · bitácoras 1:15 a.m.)");
        return "admin-jobs";
    }

    /** ▶️ Dispara manualmente los jobs de alertas (sin esperar a la 1:00 a.m.) para verificarlos */
    @PostMapping("/jobs/ejecutar")
    public String ejecutarJobAhora(HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            visitaAlertaService.revisarAlertasVisitas();
            bitacoraAlertaService.revisarBitacorasAtrasadas();
            auditoriaService.registrar(admin.getIdUsuario(), AccionAuditoria.ALERTA,
                    "Jobs de alertas (visitas y bitácoras) ejecutados manualmente desde Monitoreo de Jobs");
            redirect.addFlashAttribute("exito", "Jobs ejecutados. Revisa las filas más recientes del historial.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "El job falló: " + e.getMessage());
        }
        return "redirect:/admin/jobs";
    }

    // ══════════════════════════ AUDITORÍA ══════════════════════════

    @GetMapping("/auditoria")
    public String verAuditoria(@RequestParam(required = false) AccionAuditoria accion,
                               @RequestParam(required = false) String texto,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                               HttpSession session, Model model) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        boolean hayFiltros = accion != null || (texto != null && !texto.isBlank()) || desde != null || hasta != null;

        model.addAttribute("usuario", admin);
        model.addAttribute("acciones", AccionAuditoria.values());
        model.addAttribute("accionSeleccionada", accion);
        model.addAttribute("texto", texto == null ? "" : texto);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("registros", hayFiltros
                ? auditoriaRepository.buscarConFiltros(accion,
                        texto == null || texto.isBlank() ? null : texto.trim(),
                        desde == null ? null : desde.atStartOfDay(),
                        hasta == null ? null : hasta.atTime(23, 59, 59))
                : auditoriaRepository.findTop200ByOrderByFechaDesc());
        return "admin-auditoria";
    }

    // ══════════════════════════ CONFIGURACIÓN GLOBAL ══════════════════════════

    @GetMapping("/config")
    public String verConfiguracion(HttpSession session, Model model) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        // Garantiza las variables por defecto aunque Oracle no estuviera lista en el arranque
        configuracionGlobalService.sembrarValoresPorDefecto();

        model.addAttribute("usuario", admin);
        model.addAttribute("configuraciones", configuracionGlobalService.listar());
        model.addAttribute("modoMantenimiento",
                configuracionGlobalService.getBooleano(ConfiguracionGlobalService.MODO_MANTENIMIENTO, false));
        return "admin-config";
    }

    @PostMapping("/config/actualizar")
    public String actualizarConfiguracion(@RequestParam Long idConfiguracion,
                                          @RequestParam String valor,
                                          HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            var config = configuracionGlobalService.actualizar(idConfiguracion, valor);
            auditoriaService.registrar(admin.getIdUsuario(), AccionAuditoria.UPDATE,
                    "Configuración Global: " + config.getClave() + " = " + config.getValor());
            redirect.addFlashAttribute("exito",
                    "Variable " + config.getClave() + " actualizada en caliente (aplica de inmediato).");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/config";
    }
}
