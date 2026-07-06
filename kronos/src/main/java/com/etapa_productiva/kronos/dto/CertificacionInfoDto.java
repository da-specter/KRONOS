package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🎓 Resumen de requisitos de certificación de una Etapa Productiva: cuántas bitácoras
 * quedaron aprobadas, si el Formato de Planeación (023) fue aprobado, y cuántas visitas de
 * seguimiento se realizaron. Alimenta tanto la tarjeta de éxito del Aprendiz como la bandeja
 * "Certificación Aprendiz" del Gestor de Etapa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificacionInfoDto {
    private int totalBitacoras;
    private int bitacorasAprobadas;
    private boolean formatoAprobado;
    private int visitasRealizadas;
    private int totalVisitas;

    public boolean isBitacorasCompletas() {
        return totalBitacoras > 0 && bitacorasAprobadas == totalBitacoras;
    }
}
