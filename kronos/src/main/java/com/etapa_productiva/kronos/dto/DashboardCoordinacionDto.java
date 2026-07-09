package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 🎓 Datos ya calculados para el dashboard de Coordinación Académica: cuántos aprendices hay
 * en total, cuántos están en Etapa Productiva y cuántos no, desglosado ficha por ficha.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCoordinacionDto {

    private int totalFichas;
    private int totalAprendices;
    private int totalEnEtapa;
    private int totalSinEtapa;

    // Dona global (usada por el panel minimalista de Registro): % en Etapa Productiva
    // sobre el total de aprendices, y el conic-gradient ya armado para pintarla.
    private int porcentajeEnEtapaGlobal;
    private String donutGradientGlobal;

    private List<FichaResumen> fichas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FichaResumen {
        private String numeroFicha;
        private String programa;
        private int enEtapa;
        private int sinEtapa;
        private int total;
        // Porcentajes sobre el total de la ficha, usados para animar el ancho de las barras
        private int porcentajeEnEtapa;
        private int porcentajeSinEtapa;
    }
}
