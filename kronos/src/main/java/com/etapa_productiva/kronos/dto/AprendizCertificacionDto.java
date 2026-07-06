package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🎓 Fila de la bandeja "Certificación Aprendiz" del Gestor de Etapa: un aprendiz cuya Etapa
 * Productiva ya está en POR_CERTIFICAR, con el resumen de sus requisitos y su Instructor de
 * Seguimiento, listo para la aprobación final.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprendizCertificacionDto {
    private Long idEtapa;
    private String nombres;
    private String apellidos;
    private String documento;
    private String ficha;

    private int totalBitacoras;
    private int bitacorasAprobadas;
    private boolean formatoAprobado;
    private int visitasRealizadas;
    private int totalVisitas;

    private String instructorSeguimiento;
}
