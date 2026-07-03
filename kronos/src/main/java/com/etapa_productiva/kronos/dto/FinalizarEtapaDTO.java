package com.etapa_productiva.kronos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class FinalizarEtapaDTO {

    @NotNull(message = "El aprendiz (idAprendizFicha) es obligatorio.")
    private Long idAprendizFicha;

    @NotNull(message = "La empresa (idEmpresa) es obligatoria.")
    private Long idEmpresa;

    @NotNull(message = "El tipo de contrato (idTipoContrato) es obligatorio.")
    private Long idTipoContrato;

    @NotNull(message = "La fecha de inicio es obligatoria.")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria.")
    private LocalDate fechaFin;

    @NotBlank(message = "El nombre del jefe inmediato es obligatorio.")
    private String nombreJefeInmediato;

    @NotBlank(message = "El correo del jefe inmediato es obligatorio.")
    private String correoJefeInmediato;

    @NotBlank(message = "El teléfono del jefe inmediato es obligatorio.")
    private String telefonoJefeInmediato;
}
