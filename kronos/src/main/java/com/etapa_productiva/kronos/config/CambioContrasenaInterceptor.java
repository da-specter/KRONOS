package com.etapa_productiva.kronos.config;

import com.etapa_productiva.kronos.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 🔐 Todo usuario nuevo arranca con contraseña = su documento. Mientras la bandera
 * DEBE_CAMBIAR_CONTRASENA siga activa en su sesión, cualquier request (menos login/estáticos,
 * excluidos en WebConfig) se redirige a /auth/cambiar-contrasena-inicial, sin dejarlo usar
 * ninguna función del sistema hasta que la cambie.
 */
@Component
public class CambioContrasenaInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null || !Boolean.TRUE.equals(usuario.getDebeCambiarContrasena())) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/auth/cambiar-contrasena-inicial");
        return false;
    }
}
