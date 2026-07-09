package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📋 Fila del "Reporte Aprendiz": un aprendiz (con su solicitud de Etapa Práctica) y la lista
 * de documentos requisito que ya diligenció y que quedaron APROBADOS — evidencia de cumplimiento
 * para Registro y para el Administrador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteAprendizDto {

    private Long idSolicitud;
    private String nombre;
    private String apellido;
    private String tipoDocumento;
    private String documento;
    private String numeroFicha;
    private String programa;
    private String modalidadContrato;
    private String estadoSolicitud;

    private List<DocumentoAprobado> documentos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentoAprobado {
        private String nombreDocumento;
        private String asunto;
        private String fechaSubida;
        private String rutaArchivo;
    }
}
