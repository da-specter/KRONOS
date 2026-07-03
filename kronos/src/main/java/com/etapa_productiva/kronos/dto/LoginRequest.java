package com.etapa_productiva.kronos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    // Deben coincidir exactamente con los 'name' o 'th:field' de tu HTML
    @NotBlank(message = "El correo electrónico es obligatorio.")
    private String correoElectronico;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String contrasena;
}
