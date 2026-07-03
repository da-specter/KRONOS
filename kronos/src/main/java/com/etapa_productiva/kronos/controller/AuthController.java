package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginRequest;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.service.AuthService;
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

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

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
}