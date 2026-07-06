package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📄 Fila de la lista de bitácoras de un aprendiz, en la vista de detalle del Instructor
 * Técnico: permite descargar el archivo y ver/registrar la evaluación (resultado + novedad).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraEvaluarDto {
    private Long idBitacora;
    private Integer numeroBitacora;
    private String asunto;
    private String rutaArchivo;
    private String fechaSubida;

    // null mientras no haya evaluación registrada (aparece "En revisión" en la vista)
    private String resultado;
    private String observaciones;
}
