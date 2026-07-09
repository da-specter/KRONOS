package com.etapa_productiva.kronos.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LoginResponse {
    private Long idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private String fotoPerfil;
    private List<String> roles;
    private List<MenuDto> menuNavegacion;
    private Boolean debeCambiarContrasena;

    /**
     * 🏷️ Nombre legible del rol para mostrarlo en la interfaz (sidebar): convierte el
     * código técnico del rol (GESTOR_ETAPA) en su etiqueta oficial (Gestor de Etapa).
     * Si el usuario tiene varios roles, los une con " · ".
     */
    public String getRolesLegibles() {
        if (roles == null || roles.isEmpty()) {
            return "Sin rol asignado";
        }
        return String.join(" · ", roles.stream().map(LoginResponse::etiquetaRol).toList());
    }

    private static String etiquetaRol(String rol) {
        return switch (rol) {
            case "APRENDIZ" -> "Aprendiz";
            case "GESTOR_ETAPA" -> "Gestor de Etapa";
            case "REGISTRO" -> "Registro";
            case "INSTRUCTOR_SEGUIMIENTO" -> "Instructor de Seguimiento";
            case "INSTRUCTOR_TECNICO" -> "Instructor Técnico";
            case "COORDINADOR_ACADEMICO" -> "Coordinación Académica";
            case "ADMINISTRADOR" -> "Administrador";
            default -> rol.replace('_', ' ');
        };
    }
}