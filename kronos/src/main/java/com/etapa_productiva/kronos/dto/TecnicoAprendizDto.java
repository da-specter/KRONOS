package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 👨‍🎓 Fila del módulo "Mis Fichas" del Instructor Técnico: datos del aprendiz matriculado
 * en sus fichas, su situación (en etapa práctica, sin etapa, por certificar, certificado)
 * y la información de su Etapa Productiva si la tiene (empresa, contrato, modalidad, fechas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TecnicoAprendizDto {
    private Long idAprendizFicha;
    private String nombres;
    private String apellidos;
    private String tipoDocumento;
    private String documento;
    private String telefono;
    private String correoElectronico;
    private String ficha;
    private String programaFormacion;
    private String estadoAcademico;

    // Situación consolidada para el dashboard: "En etapa práctica", "Sin etapa práctica",
    // "Por certificar" o "Certificado"
    private String situacion;

    // Datos de la Etapa Productiva (— si aún no tiene una registrada)
    private String empresa;
    private String modalidadContrato; // TipoContrato (Contrato de aprendizaje, pasantía, etc.)
    private String modalidad;         // Presencial / Remoto / Híbrido
    private String etapaInicio;
    private String etapaFin;
    private String estadoEtapa;
}
