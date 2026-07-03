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
}