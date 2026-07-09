package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginRequest;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.service.AuthService;
import com.etapa_productiva.kronos.service.PasswordRecoveryService;
import com.etapa_productiva.kronos.service.RegistroPublicoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordRecoveryService passwordRecoveryService;

    @Autowired
    private RegistroPublicoService registroPublicoService;

    @GetMapping("/login")
    public String verLogin(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@Valid @ModelAttribute("loginRequest") LoginRequest request, BindingResult bindingResult, HttpSession session, Model model) {
        if (bindingResult.hasErrors()) {
            request.setContrasena(null);
            model.addAttribute("error", "Debes ingresar tu correo y contraseña.");
            return "login";
        }

        try {
            System.out.println("🎮 [CONTROLLER] Petición recibida de: " + request.getCorreoElectronico());

            LoginResponse respuesta = authService.iniciarSesion(request);
            session.setAttribute("usuarioSesion", respuesta);

            // 🚨 CAMBIO AQUÍ: Ahora redirige a /index en lugar de /dashboard
            System.out.println("🚀 [CONTROLLER] Autenticación Exitosa. Redireccionando al index...");
            return "redirect:/index";

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            System.out.println("⚠️ [CONTROLLER] Credenciales denegadas: " + e.getMessage());
            request.setContrasena(null);
            model.addAttribute("error", e.getMessage());
            return "login";
        } catch (Exception e) {
            System.out.println("💥 [CRITICAL ERROR] Fallo inesperado en el sistema:");
            e.printStackTrace();
            request.setContrasena(null);
            model.addAttribute("error", "Error interno en el servidor: " + e.getMessage());
            return "login";
        }
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }

    /**
     * 🔐 Cambio de contraseña obligatorio del primer ingreso (contraseña por defecto =
     * documento). El interceptor CambioContrasenaInterceptor redirige aquí cualquier
     * request mientras la bandera siga activa.
     */
    @GetMapping("/cambiar-contrasena-inicial")
    public String verCambioContrasenaInicial(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        if (!Boolean.TRUE.equals(usuarioLogueado.getDebeCambiarContrasena())) {
            return "redirect:/index";
        }
        model.addAttribute("nombre", usuarioLogueado.getNombre());
        return "cambiar-contrasena-inicial";
    }

    @PostMapping("/cambiar-contrasena-inicial")
    public String procesarCambioContrasenaInicial(
            @RequestParam String nuevaContrasena,
            @RequestParam String confirmarContrasena,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        if (!Boolean.TRUE.equals(usuarioLogueado.getDebeCambiarContrasena())) {
            return "redirect:/index";
        }

        model.addAttribute("nombre", usuarioLogueado.getNombre());

        if (nuevaContrasena == null || nuevaContrasena.isBlank()) {
            model.addAttribute("error", "Debes ingresar una nueva contraseña.");
            return "cambiar-contrasena-inicial";
        }
        if (!nuevaContrasena.equals(confirmarContrasena)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "cambiar-contrasena-inicial";
        }

        try {
            authService.cambiarContrasenaInicial(usuarioLogueado.getIdUsuario(), nuevaContrasena);
            session.invalidate();
            redirectAttributes.addFlashAttribute("exito", "Tu contraseña se actualizó correctamente. Ya puedes iniciar sesión.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "cambiar-contrasena-inicial";
        }
    }

    @GetMapping("/recuperar")
    public String verRecuperar() {
        return "recuperar-contrasena";
    }

    /**
     * 🆕 Autorregistro público: cubre a los aprendices o instructores que el Gestor/Admin no
     * alcanzó a importar. El parámetro `tipo` (aprendiz|instructor) decide qué panel del
     * formulario se muestra abierto (por defecto aprendiz).
     */
    @GetMapping("/registro")
    public String verRegistro(@RequestParam(required = false, defaultValue = "aprendiz") String tipo, Model model) {
        model.addAttribute("tiposDocumento", TipoDocumento.values());
        model.addAttribute("tipoSeleccionado", tipo);
        return "registro";
    }

    @PostMapping("/registro/aprendiz")
    public String registrarAprendiz(
            @RequestParam TipoDocumento tipoDocumento,
            @RequestParam String documento,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam String numeroFicha,
            @RequestParam String contrasena,
            @RequestParam String confirmarContrasena,
            RedirectAttributes redirectAttributes) {

        try {
            registroPublicoService.registrarAprendiz(tipoDocumento, documento, nombre, apellido, correo,
                    telefono, numeroFicha, contrasena, confirmarContrasena);
            redirectAttributes.addFlashAttribute("exito",
                    "¡Cuenta creada! Ya puedes iniciar sesión con tu correo y tu contraseña.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("tipo", "aprendiz");
            return "redirect:/auth/registro";
        }
    }

    @PostMapping("/registro/instructor")
    public String registrarInstructor(
            @RequestParam TipoDocumento tipoDocumento,
            @RequestParam String documento,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam String contrasena,
            @RequestParam String confirmarContrasena,
            RedirectAttributes redirectAttributes) {

        try {
            registroPublicoService.registrarSolicitudInstructor(tipoDocumento, documento, nombre, apellido,
                    correo, telefono, contrasena, confirmarContrasena);
            redirectAttributes.addFlashAttribute("exito",
                    "Tu solicitud fue enviada. Un Administrador revisará tu cuenta y te asignará tu rol; "
                            + "podrás iniciar sesión en cuanto sea aprobada.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("tipo", "instructor");
            return "redirect:/auth/registro";
        }
    }

    @PostMapping("/recuperar/enviar")
    public String enviarCodigoRecuperacion(
            @RequestParam String correoElectronico,
            Model model) {
        try {
            passwordRecoveryService.solicitarCodigoPorCorreo(correoElectronico);
            model.addAttribute("correoElectronico", correoElectronico);
            return "restablecer-contrasena";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "recuperar-contrasena";
        }
    }

    @PostMapping("/recuperar/restablecer")
    public String restablecerContrasena(
            @RequestParam String correoElectronico,
            @RequestParam String codigo,
            @RequestParam String nuevaContrasena,
            @RequestParam String confirmarContrasena,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (!nuevaContrasena.equals(confirmarContrasena)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("correoElectronico", correoElectronico);
            return "restablecer-contrasena";
        }

        try {
            passwordRecoveryService.restablecerContrasena(correoElectronico, codigo, nuevaContrasena);
            redirectAttributes.addFlashAttribute("exito", "Tu contraseña se restableció correctamente. Ya puedes iniciar sesión.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("correoElectronico", correoElectronico);
            return "restablecer-contrasena";
        }
    }
}