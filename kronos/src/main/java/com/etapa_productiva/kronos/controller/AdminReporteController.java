package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.repository.SeccionFormatoRepository;
import com.etapa_productiva.kronos.service.ReporteAprendizService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

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
}
