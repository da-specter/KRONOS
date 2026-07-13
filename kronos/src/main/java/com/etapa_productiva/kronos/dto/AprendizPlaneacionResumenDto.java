package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📋 Fila del listado "Formato Planeación 023" del módulo Evaluación de Formatos (Instructor de
 * Seguimiento): el aprendiz y el estado de sus 3 "momentos" (cada uno se habilita con un grupo
 * de 4 bitácoras). Cuando los 3 quedan escritos, KRONOS genera el PDF del 023 automáticamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprendizPlaneacionResumenDto {
    private Long idEtapa;
    private String nombres;
    private String apellidos;
    private String documento;
    private String ficha;

    private List<MomentoEstadoDto> momentos;
    private int momentosCompletados; // 0-3, para el filtro por chips

    private boolean formatoGenerado;
    private String rutaArchivo023;
    private String fechaGeneracion023;
}
