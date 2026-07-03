package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🎓 Fila del módulo "Gestión Aprendices" del Gestor de Etapa: consolida la información
 * del aprendiz, su ficha/programa, su Etapa Productiva, la empresa coformadora y el
 * estado del documento ARL. Los campos de texto se pintan y exportan (Excel/PDF) sin
 * conversiones, en tres bloques: instructor → aprendiz/programa → empresa/modalidad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprendizGestionDto {

    // 1️⃣ Instructor de seguimiento vigente
    private String instructorSeguimiento;

    // 2️⃣ Información del aprendiz y su programa
    private String tipoDocumento;
    private String documento;
    private String apellidos;
    private String nombres;
    private String telefono;
    private String correoElectronico;
    private String nivelFormacion;
    private String programaFormacion;
    private String ficha;
    private String fechaFinFicha;

    // 3️⃣ Datos de la empresa y la modalidad del contrato
    private String razonSocial;
    private String municipioEmpresa;
    private String departamentoEmpresa;
    private String correoEmpresa;
    private String telefonoEmpresa;
    private String modalidadContrato;
    private String contratoInicio;
    private String contratoFin;
    private String arl;

    // Soporte del cuadro ARL en la vista (no se exportan)
    private String arlRuta;   // ruta del archivo subido, para verlo
    private Long idEtapa;     // habilita la subida solo si el aprendiz ya tiene etapa
}
