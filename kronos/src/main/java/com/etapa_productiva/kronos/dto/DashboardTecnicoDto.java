package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📊 Datos ya calculados para el dashboard del Instructor Técnico (líder de ficha):
 * cuántos de sus aprendices están en etapa práctica, cuántos no, cuántos están por
 * certificar/certificados, y las series listas para pintar como gráficas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTecnicoDto {

    private int totalAprendices;
    private int enEtapa;
    private int sinEtapa;
    private int porCertificar;
    private int certificados;
    private int totalFichas;

    // Gráfica de dona: distribución por situación (CSS conic-gradient ya construido + leyenda)
    private String donutGradient;
    private List<DashboardInstructorDto.SegmentoGrafico> segmentosSituacion;

    // Gráfica de barras: aprendices por ficha
    private List<DashboardInstructorDto.BarraGrafico> aprendicesPorFicha;
}
