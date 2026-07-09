package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import com.etapa_productiva.kronos.repository.DepartamentoRepository;
import com.etapa_productiva.kronos.repository.MunicipioRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.service.KronosWorkflowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * 🏢 Módulo "Registro Etapa Productiva" del rol REGISTRO: registra formalmente la
 * Etapa Productiva (empresa, tipo de contrato, fechas, jefe inmediato) de una solicitud
 * que ya fue calificada por el Gestor de Etapa y validada por Registro, dejándola en
 * APROBADO_EN_ETAPA.
 */
@Controller
public class RegistroEtapaController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private MunicipioRepository municipioRepository;

    @Autowired
    private KronosWorkflowService workflowService;

    @Autowired
    private com.etapa_productiva.kronos.repository.TipoContratoRepository tipoContratoRepository;

    @Autowired
    private com.etapa_productiva.kronos.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private com.etapa_productiva.kronos.service.AuthService authService;

    @GetMapping("/registro/registro-etapa")
    public String verRegistroEtapa(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("REGISTRO")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("solicitudesParaRegistrar", solicitudRepository.findByEstado(EstadoSolicitud.LISTO_PARA_REGISTRO));

        // 🗺️ Catálogo de división territorial para los selects de Municipio/Departamento del formulario
        model.addAttribute("departamentos", departamentoRepository.findAll().stream()
                .sorted((a, b) -> a.getNombreDepartamento().compareToIgnoreCase(b.getNombreDepartamento()))
                .toList());
        model.addAttribute("municipios", municipioRepository.findAll().stream()
                .sorted((a, b) -> a.getNombreMunicipio().compareToIgnoreCase(b.getNombreMunicipio()))
                .toList());

        // 📄 Catálogo de modalidades de contrato para el select del formulario
        model.addAttribute("tiposContrato", tipoContratoRepository.findByEstadoTrueOrderByNombreTipoContratoAsc());

        return "registro-etapa";
    }

    @PostMapping("/registro/registro-etapa/{idSolicitud}")
    public String registrarEtapa(
            @PathVariable Long idSolicitud,
            @RequestParam Long idAprendizFicha,
            @RequestParam String nit,
            @RequestParam String nombreEmpresa,
            @RequestParam String direccionEmpresa,
            @RequestParam(required = false) String telefonoEmpresa,
            @RequestParam(required = false) String correoEmpresa,
            @RequestParam String nombreMunicipio,
            @RequestParam String nombreDepartamento,
            @RequestParam String nombreTipoContrato,
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin,
            @RequestParam String nombreJefeInmediato,
            @RequestParam String correoJefeInmediato,
            @RequestParam String telefonoJefeInmediato,
            @RequestParam String contrasena,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("REGISTRO")) {
            redirectAttributes.addFlashAttribute("error", "Solo el rol Registro puede registrar la Etapa Productiva.");
            return "redirect:/index";
        }

        // 🔐 Re-autenticación: antes de registrar se confirma la contraseña del usuario en sesión
        com.etapa_productiva.kronos.entity.Usuario usuarioActual =
                usuarioRepository.findById(usuarioLogueado.getIdUsuario()).orElse(null);
        if (!authService.verificarContrasena(usuarioActual, contrasena)) {
            redirectAttributes.addFlashAttribute("error",
                    "Contraseña incorrecta: no se registró la Etapa Productiva. Verifícala e inténtalo de nuevo.");
            return "redirect:/registro/registro-etapa";
        }

        try {
            com.etapa_productiva.kronos.util.ValidacionCampos.validarNit(nit);
            com.etapa_productiva.kronos.util.ValidacionCampos.validarNombreEmpresa(nombreEmpresa, "El nombre de la empresa");
            com.etapa_productiva.kronos.util.ValidacionCampos.validarTelefono(telefonoEmpresa, "El teléfono de la empresa");
            com.etapa_productiva.kronos.util.ValidacionCampos.validarCorreo(correoEmpresa, "El correo de la empresa");
            com.etapa_productiva.kronos.util.ValidacionCampos.validarNombre(nombreJefeInmediato, "El nombre del jefe inmediato");
            com.etapa_productiva.kronos.util.ValidacionCampos.validarCorreo(correoJefeInmediato, "El correo del jefe inmediato");
            com.etapa_productiva.kronos.util.ValidacionCampos.validarTelefono(telefonoJefeInmediato, "El teléfono del jefe inmediato");

            workflowService.registrarEtapaProductiva(
                    idSolicitud, idAprendizFicha,
                    nit, nombreEmpresa, direccionEmpresa, telefonoEmpresa, correoEmpresa,
                    nombreMunicipio, nombreDepartamento, nombreTipoContrato,
                    fechaInicio, fechaFin, nombreJefeInmediato, correoJefeInmediato, telefonoJefeInmediato,
                    usuarioLogueado.getIdUsuario());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/registro/registro-etapa";
    }
}
