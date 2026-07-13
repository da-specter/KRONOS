package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📋 Una de las 13 variables (Técnicas/Actitudinales) de un Momento 2 o 3 del Formato 023,
 * con la valoración y observación que el aprendiz ya haya diligenciado (o null si aún no).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactorEstadoDto {
    private Long idFactor;
    private String nombreVariable;
    private String valoracion; // "SATISFACTORIO" | "POR_MEJORAR" | null
    private String observacion;
}
