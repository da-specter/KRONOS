package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.ResultadoImportacionAdmin;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.RolRepository;
import com.etapa_productiva.kronos.service.AdminUsuariosService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 👥 Módulo "Control de Accesos" del Administrador:
 * Usuarios del Sistema (registro, roles, activación, carga masiva por rol)
 * y Soporte de Credenciales (blanqueo/reseteo de contraseñas).
 */
@Controller
@RequestMapping("/admin")
public class AdminUsuariosController {

    @Autowired private AdminUsuariosService adminUsuariosService;
    @Autowired private RolRepository rolRepository;

    // ══════════════════════════ USUARIOS DEL SISTEMA ══════════════════════════

    @GetMapping("/usuarios")
    public String verUsuarios(HttpSession session, Model model) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        model.addAttribute("usuario", admin);
        model.addAttribute("usuarios", adminUsuariosService.listarUsuarios());
        model.addAttribute("solicitudesPendientes", adminUsuariosService.listarSolicitudesPendientes());
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("tiposDocumento", TipoDocumento.values());
        return "admin-usuarios";
    }

    @PostMapping("/usuarios/crear")
    public String crearUsuario(@RequestParam TipoDocumento tipoDocumento,
                               @RequestParam String documento,
                               @RequestParam String nombre,
                               @RequestParam String apellido,
                               @RequestParam String correo,
                               @RequestParam(required = false) String telefono,
                               @RequestParam String nombreRol,
                               HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            adminUsuariosService.crearUsuario(tipoDocumento, documento, nombre, apellido, correo, telefono, nombreRol, admin.getIdUsuario());
            redirect.addFlashAttribute("exito",
                    "Usuario creado correctamente. Su contraseña inicial es su número de documento.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/rol/asignar")
    public String asignarRol(@RequestParam Long idUsuario,
                             @RequestParam String nombreRol,
                             HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            adminUsuariosService.asignarRol(idUsuario, nombreRol, admin.getIdUsuario(), true);
            redirect.addFlashAttribute("exito", "Rol asignado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/rol/quitar")
    public String quitarRol(@RequestParam Long idUsuario,
                            @RequestParam String nombreRol,
                            HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            adminUsuariosService.quitarRol(idUsuario, nombreRol, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Rol retirado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/estado")
    public String cambiarEstado(@RequestParam Long idUsuario,
                                @RequestParam boolean activar,
                                HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            adminUsuariosService.cambiarEstado(idUsuario, activar, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Cuenta " + (activar ? "activada" : "desactivada") + " correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping(value = "/usuarios/importar", consumes = "multipart/form-data")
    public String importarUsuarios(@RequestParam("archivo") MultipartFile archivo,
                                   @RequestParam String nombreRol,
                                   HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            ResultadoImportacionAdmin resultado = adminUsuariosService.importarUsuarios(archivo, nombreRol, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Carga masiva de usuarios (" + nombreRol + ") finalizada. " + resultado.resumen());
            if (!resultado.errores().isEmpty()) {
                redirect.addFlashAttribute("erroresImportacion", resultado.errores());
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ══════════════════════════ SOLICITUDES DE ACCESO (AUTORREGISTRO) ══════════════════════════

    @PostMapping("/usuarios/solicitudes/aprobar")
    public String aprobarSolicitud(@RequestParam Long idUsuario,
                                   @RequestParam String nombreRol,
                                   HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            adminUsuariosService.aprobarSolicitud(idUsuario, nombreRol, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Solicitud aprobada: la cuenta quedó activa con el rol " + nombreRol + ".");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/solicitudes/rechazar")
    public String rechazarSolicitud(@RequestParam Long idUsuario,
                                    HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            adminUsuariosService.rechazarSolicitud(idUsuario, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Solicitud rechazada y eliminada.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ══════════════════════════ SOPORTE DE CREDENCIALES ══════════════════════════

    @GetMapping("/credenciales")
    public String verCredenciales(@RequestParam(required = false) String criterio,
                                  HttpSession session, Model model) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        model.addAttribute("usuario", admin);
        model.addAttribute("criterio", criterio == null ? "" : criterio);

        Usuario encontrado = adminUsuariosService.buscarPorDocumentoOCorreo(criterio);
        model.addAttribute("usuarioEncontrado", encontrado);
        if (criterio != null && !criterio.isBlank() && encontrado == null) {
            model.addAttribute("error", "No existe un usuario con documento o correo '" + criterio.trim() + "'.");
        }
        return "admin-credenciales";
    }

    @PostMapping("/credenciales/reset")
    public String resetearContrasena(@RequestParam String documentoOCorreo,
                                     @RequestParam(required = false) String nuevaContrasena,
                                     @RequestParam(required = false, defaultValue = "false") boolean reactivarCuenta,
                                     HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            Usuario usuario = adminUsuariosService.resetearContrasena(documentoOCorreo, nuevaContrasena, reactivarCuenta, admin.getIdUsuario());
            boolean blanqueo = nuevaContrasena == null || nuevaContrasena.isBlank();
            redirect.addFlashAttribute("exito", "Credenciales de " + usuario.getNombre() + " " + usuario.getApellido()
                    + " actualizadas. " + (blanqueo ? "La contraseña vuelve a ser su número de documento." : "Contraseña nueva aplicada."));
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/credenciales";
    }
}
