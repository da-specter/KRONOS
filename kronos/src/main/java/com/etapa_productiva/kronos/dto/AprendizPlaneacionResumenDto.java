package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📋 Fila del listado "Formato Planeación 023" del módulo Evaluación de Formatos
 * (Instructor Técnico): el aprendiz, su Formato de Planeación (si ya lo radicó) y el
 * estado de su evaluación (Sin radicar, En revisión, Aprobado, Reprobado, Para corregir).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprendizPlaneacionResumenDto {
    private Long idEtapa;
    private Long idFormatoPlaneacion; // null si el aprendiz aún no lo ha radicado
    private String nombres;
    private String apellidos;
    private String documento;
    private String ficha;

    private String asunto;
    private String fechaSubida;
    private String rutaArchivo;

    private String estado; // Sin radicar / En revisión / Aprobado / Reprobado / Para corregir
    private String observaciones; // última novedad registrada, si la hay
}
