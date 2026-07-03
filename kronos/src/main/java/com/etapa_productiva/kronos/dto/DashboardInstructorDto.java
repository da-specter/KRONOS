package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📊 Datos ya calculados para el dashboard del Instructor de Seguimiento en el index:
 * números clave y las series listas para pintar como gráficas (sin librerías externas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardInstructorDto {

    private int totalAprendices;
    private int enProgreso;
    private int aprobados;
    private int reprobados;
    private int enSuspension;
    private int totalFichas;
    private int totalEmpresas;

    // Gráfica de dona: distribución por estado de etapa (CSS conic-gradient ya construido + leyenda)
    private String donutGradient;
    private List<SegmentoGrafico> segmentosEstado;

    // Gráfica de barras: aprendices por ficha
    private List<BarraGrafico> aprendicesPorFicha;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentoGrafico {
        private String etiqueta;
        private int cantidad;
        private int porcentaje;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BarraGrafico {
        private String etiqueta;
        private int cantidad;
        private int porcentaje; // ancho relativo respecto al máximo, 0-100
    }
}
