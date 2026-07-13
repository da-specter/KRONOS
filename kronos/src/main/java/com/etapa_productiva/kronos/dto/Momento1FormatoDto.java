package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📋 Datos del Momento 1 para la plantilla HTML `fragments/PlantillaPlaneacion023`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Momento1FormatoDto {
    private String fechaInicio;
    private String fechaFin;
    private String fechaArl;
    private String numeroPoliza;
    private String horario;
    private String enlaceGrabacion;
    private String competencias;
    private String resultados;
    private String actividades;
    private String evidencias;
    private String observaciones;

    // Cierre "Ciudad ___ y fecha ___ de forma presencial/virtual"
    private String ciudad;
    private String fechaDia;
    private String fechaMes;
    private String fechaAnio;
    private String modalidadCierre; // "Presencial" | "Virtual"
}
