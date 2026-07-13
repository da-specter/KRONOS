package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📋 Datos del Momento 3 (variable "evaluacion" en la plantilla HTML `fragments/PlantillaPlaneacion023`).
 * Los 13 valores de factor son "SATISFACTORIO" | "POR_MEJORAR" | null, y {@code juicioFinal} es
 * "APROBADO" | "NO_APROBADO" | null (mismo criterio que {@code JuicioEvaluacion}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionMomento3Dto {
    private String fechaInicio;
    private String fechaFin;
    private String numeroVisitas;
    private String modalidad;
    private String enlaceGrabacion;

    // Factores Técnicos
    private String aplicacionConocimiento;
    private String obsConocimiento;
    private String mejoraContinua;
    private String obsMejora;
    private String fortalecimiento;
    private String obsFortalecimiento;
    private String oportunidad;
    private String obsOportunidad;
    private String responsabilidad;
    private String obsResponsabilidad;
    private String administracion;
    private String obsAdministracion;
    private String seguridad;
    private String obsSeguridad;
    private String documentacion;
    private String obsDocumentacion;

    // Factores Actitudinales y Comportamentales
    private String relaciones;
    private String obsRelaciones;
    private String equipo;
    private String obsEquipo;
    private String solucion;
    private String obsSolucion;
    private String cumplimiento;
    private String obsCumplimiento;
    private String organizacion;
    private String obsOrganizacion;

    private String retroProcesoCoformador;
    private String retroDesempenoCoformador;
    private String retroProcesoInstructor;
    private String retroDesempenoInstructor;
    private String retroProcesoAprendiz;
    private String retroDesempenoAprendiz;
    private String juicioFinal;

    // Cierre "ciudad ___ con fecha ___ de forma presencial/virtual"
    private String ciudad;
    private String fechaDia;
    private String fechaMes;
    private String fechaAnio;
    private String modalidadCierre; // "Presencial" | "Virtual"
}
