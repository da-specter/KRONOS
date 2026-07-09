package com.etapa_productiva.kronos.dto;

import com.etapa_productiva.kronos.entity.EstadoEtapaAprendiz;
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
    private String registradoPor; // Usuario del rol Registro que dio de alta la Etapa Productiva
    private String fechaRegistro; // Momento real en que se confirmó el registro (auditoría), no la fecha contractual
    private String mesRegistro;   // Clave "yyyy-MM" de fechaRegistro, para el filtro "por mes" (null si no tiene etapa)

    // Valores "crudos" (sin formatear) que alimentan el formulario de edición del rol Registro
    private String nitEmpresa;
    private String jefeNombre;
    private String jefeCorreo;
    private String jefeTelefono;
    private String estadoEtapaEtapa;   // nombre real del enum EtapaProductiva.estadoEtapa
    private String contratoInicioIso;  // yyyy-MM-dd, para precargar <input type="date">
    private String contratoFinIso;

    // Soporte del cuadro ARL en la vista (no se exportan)
    private String arlRuta;   // ruta del archivo subido, para verlo
    private Long idEtapa;     // habilita la subida solo si el aprendiz ya tiene etapa

    // 🚦 Clasificación semáforo (formato SENA) según dónde está el aprendiz en la línea de
    // tiempo de su Etapa Práctica: antes de iniciar, en curso, o después de terminar.
    // El texto ya trae nombrada la modalidad real que escogió el aprendiz (Pasantía,
    // Vínculo Laboral, Monitoría, etc.), no un genérico "contrato de aprendizaje".
    private EstadoEtapaAprendiz estadoEtapa;
    private String estadoEtapaTexto;
}
