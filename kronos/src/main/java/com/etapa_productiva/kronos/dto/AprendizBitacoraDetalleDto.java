package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 👨‍🎓 Vista de detalle del Instructor Técnico al entrar a un aprendiz desde "Bitácoras":
 * sus datos básicos y la lista completa de bitácoras que ha radicado, listas para
 * descargar y evaluar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprendizBitacoraDetalleDto {
    private Long idEtapa;
    private String nombres;
    private String apellidos;
    private String documento;
    private String ficha;
    private List<BitacoraEvaluarDto> bitacoras;
}
