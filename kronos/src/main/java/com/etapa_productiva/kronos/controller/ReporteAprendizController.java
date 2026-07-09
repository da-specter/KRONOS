package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SeccionFormatoRepository;
import com.etapa_productiva.kronos.service.ReporteAprendizService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

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
