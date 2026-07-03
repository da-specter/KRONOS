package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 👤 Módulo "Mi Perfil": disponible para cualquier rol autenticado. Permite ver y editar
 * los propios datos guardados en la entidad Usuario (nombre, apellido, correo, teléfono
 * y contraseña).
 */
@Controller
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/perfil")
    public String verPerfil(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        Usuario perfil = usuarioRepository.findById(usuarioLogueado.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("perfil", perfil);
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        return "perfil";
    }

    @PostMapping("/perfil/editar")
    public String editarPerfil(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String correoElectronico,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String contrasenaActual,
            @RequestParam(required = false) String contrasenaNueva,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        try {
            Usuario usuario = usuarioRepository.findById(usuarioLogueado.getIdUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

            usuarioRepository.findByCorreoElectronico(correoElectronico).ifPresent(otro -> {
                if (!otro.getIdUsuario().equals(usuario.getIdUsuario())) {
                    throw new IllegalArgumentException("Ese correo ya está en uso por otro usuario.");
                }
            });

            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setCorreoElectronico(correoElectronico);
            usuario.setTelefono(telefono);

            if (contrasenaNueva != null && !contrasenaNueva.isBlank()) {
                if (contrasenaActual == null || contrasenaActual.isBlank()) {
                    throw new IllegalArgumentException("Debes ingresar tu contraseña actual para cambiarla.");
                }
                String passwordGuardada = usuario.getPassword();
                boolean coincide = (passwordGuardada != null
                        && (passwordGuardada.startsWith("$2a$") || passwordGuardada.startsWith("$2b$") || passwordGuardada.startsWith("$2y$")))
                        ? passwordEncoder.matches(contrasenaActual, passwordGuardada)
                        : contrasenaActual.equals(passwordGuardada);
                if (!coincide) {
                    throw new IllegalArgumentException("La contraseña actual no es correcta.");
                }
                usuario.setPassword(passwordEncoder.encode(contrasenaNueva));
            }

            usuarioRepository.save(usuario);

            usuarioLogueado.setNombre(usuario.getNombre());
            usuarioLogueado.setApellido(usuario.getApellido());
            usuarioLogueado.setCorreo(usuario.getCorreoElectronico());
            session.setAttribute("usuarioSesion", usuarioLogueado);

            redirectAttributes.addFlashAttribute("exito", "Tu perfil se actualizó correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/perfil";
    }
}
