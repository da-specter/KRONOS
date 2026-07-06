package com.etapa_productiva.kronos.dto;

import java.util.List;

/**
 * 👥 Fila de la tabla "Usuarios del Sistema" del Administrador: los datos del usuario
 * más sus roles ya resueltos (evita tocar colecciones LAZY desde la vista Thymeleaf).
 */
public record UsuarioAdminDto(
        Long idUsuario,
        String tipoDocumento,
        String documento,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        Boolean estado,
        List<String> roles) {

    public String rolesTexto() {
        return roles == null || roles.isEmpty() ? "Sin rol" : String.join(", ", roles);
    }
}
