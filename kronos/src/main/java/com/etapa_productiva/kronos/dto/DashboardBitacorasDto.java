package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📊 Datos ya calculados para el dashboard de "Bitácoras" del Instructor Técnico: cuántas
 * bitácoras han subido en total sus aprendices y cómo se distribuyen por resultado
 * (en revisión, aprobadas, para corregir, reprobadas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardBitacorasDto {

    private int totalSubidas;
    private int aprobadas;
    private int enRevision;
    private int corregir;
    private int reprobadas;

    // Gráfica de dona: distribución por resultado (CSS conic-gradient ya construido + leyenda)
    private String donutGradient;
    private List<DashboardInstructorDto.SegmentoGrafico> segmentosResultado;

    // Gráfica de barras: bitácoras subidas por aprendiz
    private List<DashboardInstructorDto.BarraGrafico> bitacorasPorAprendiz;
}
