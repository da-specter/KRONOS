package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 👨‍🎓 Fila del módulo "Mis Aprendices" del Instructor de Seguimiento: los datos del aprendiz
 * y de su Etapa Productiva (empresa, modalidad, fechas y estado) para listar y exportar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorAprendizDto {
    private Long idEtapa;
    private String nombres;
    private String apellidos;
    private String tipoDocumento;
    private String documento;
    private String telefono;
    private String correoElectronico;
    private String ficha;
    private String programaFormacion;
    private String razonSocial;
    private String modalidadContrato;
    private String etapaInicio;
    private String etapaFin;
    private String estadoEtapa;
}
