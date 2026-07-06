package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📓 Fila del listado "Bitácoras" del módulo Evaluación de Formatos (Instructor Técnico):
 * un aprendiz de sus fichas con el resumen de cuántas bitácoras ha subido y en qué resultado
 * quedó cada una (en revisión, aprobada, para corregir, reprobada).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprendizBitacoraResumenDto {
    private Long idEtapa;
    private String nombres;
    private String apellidos;
    private String documento;
    private String ficha;

    private int totalSubidas;
    private int aprobadas;
    private int enRevision;
    private int corregir;
    private int reprobadas;
}
