package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.ModalidadFirma;
import com.etapa_productiva.kronos.service.EvaluacionFormatosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * 📋 Lado Aprendiz del Formato 023: por cada uno de los 3 "momentos" ya habilitados, el aprendiz
 * digita su parte (planeación en el Momento 1, observación propia en cualquier momento) y sube
 * las firmas (propia y del ente co-formador) que se reutilizan en las 3 secciones del PDF.
 */
@Controller
public class AprendizPlaneacionController {

    @Autowired
    private EvaluacionFormatosService evaluacionFormatosService;

    @PostMapping(value = "/aprendiz/planeacion/momento", consumes = "multipart/form-data")
    public String guardarDatosMomento(
            @RequestParam Long idEtapa,
            @RequestParam int numeroMomento,
            @RequestParam(required = false) String competenciasDesarrollar,
            @RequestParam(required = false) String resultadosAprendizaje,
            @RequestParam(required = false) String actividadesDesarrollar,
            @RequestParam(required = false) String evidenciaDescripcion,
            @RequestParam(value = "evidenciaArchivo", required = false) MultipartFile evidenciaArchivo,
            @RequestParam(required = false) String observacionAprendiz,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDiligenciamiento,
            @RequestParam(required = false) ModalidadFirma modalidadFirma,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaMomento,
            @RequestParam(required = false) String observacionEnteCoformador,
            @RequestParam(required = false) String retroAprendizProceso,
            @RequestParam(required = false) String retroAprendizDesempeno,
            @RequestParam(required = false) String retroEnteProceso,
            @RequestParam(required = false) String retroEnteDesempeno,
            @RequestParam(required = false) List<Long> idFactor,
            @RequestParam(required = false) List<String> valoracionFactor,
            @RequestParam(required = false) List<String> observacionFactor,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = validarAcceso(session);
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        if (!esAprendiz(usuarioLogueado)) {
            return "redirect:/index";
        }

        try {
            evaluacionFormatosService.guardarDatosAprendizMomento(usuarioLogueado.getIdUsuario(), idEtapa, numeroMomento,
                    competenciasDesarrollar, resultadosAprendizaje, actividadesDesarrollar,
                    evidenciaDescripcion, evidenciaArchivo, observacionAprendiz,
                    ciudad, fechaDiligenciamiento, modalidadFirma,
                    fechaMomento, observacionEnteCoformador,
                    retroAprendizProceso, retroAprendizDesempeno, retroEnteProceso, retroEnteDesempeno,
                    idFactor, valoracionFactor, observacionFactor);
            redirectAttributes.addFlashAttribute("exito", "Datos del Momento " + numeroMomento + " guardados y notificados a tu instructor.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/aprendiz/bitacoras";
    }

    @PostMapping(value = "/aprendiz/planeacion/correo-institucional")
    public String guardarCorreoInstitucional(
            @RequestParam Long idEtapa,
            @RequestParam String correoInstitucional,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = validarAcceso(session);
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        if (!esAprendiz(usuarioLogueado)) {
            return "redirect:/index";
        }

        try {
            evaluacionFormatosService.guardarCorreoInstitucional(usuarioLogueado.getIdUsuario(), idEtapa, correoInstitucional);
            redirectAttributes.addFlashAttribute("exito", "Correo institucional guardado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/aprendiz/bitacoras";
    }

    @PostMapping(value = "/aprendiz/planeacion/firmas", consumes = "multipart/form-data")
    public String guardarFirmas(
            @RequestParam Long idEtapa,
            @RequestParam(value = "firmaAprendiz", required = false) MultipartFile firmaAprendiz,
            @RequestParam(value = "firmaEmpresa", required = false) MultipartFile firmaEmpresa,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = validarAcceso(session);
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        if (!esAprendiz(usuarioLogueado)) {
            return "redirect:/index";
        }

        try {
            evaluacionFormatosService.guardarFirmaAprendiz(usuarioLogueado.getIdUsuario(), idEtapa, firmaAprendiz, firmaEmpresa);
            redirectAttributes.addFlashAttribute("exito", "Firmas guardadas correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/aprendiz/bitacoras";
    }

    private LoginResponse validarAcceso(HttpSession session) {
        return (LoginResponse) session.getAttribute("usuarioSesion");
    }

    private boolean esAprendiz(LoginResponse usuario) {
        List<String> roles = usuario.getRoles();
        return roles != null && roles.contains("APRENDIZ");
    }
}
