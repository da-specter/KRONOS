package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Entity
@Table(name = "ETAPA_PRODUCTIVA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaProductiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // GENERATED AS IDENTITY nativo de Oracle 12c+
    @Column(name = "ID_ETAPA", columnDefinition = "NUMBER(19,0)")
    private Long idEtapa;

    // Relación Muchos a Uno: Conecta con la matrícula del aprendiz en la ficha (5FN)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_APRENDIZ_FICHA", 
        referencedColumnName = "ID_APRENDIZ_FICHA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private AprendizFicha aprendizFicha;

    // Relación Muchos a Uno: Conecta con la empresa coformadora
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_EMPRESA", 
        referencedColumnName = "ID_EMPRESA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Empresa empresa;

    // Relación Muchos a Uno: Conecta con la modalidad (Contrato de aprendizaje, pasantía, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_TIPO_CONTRATO", 
        referencedColumnName = "ID_TIPO_CONTRATO", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private TipoContrato tipoContrato;

    @Column(name = "FECHA_INICIO", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "FECHA_FIN", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODALIDAD", columnDefinition = "VARCHAR2(20)", nullable = false)
    private ModalidadEtapa modalidad; // 🚀 ¡Ahora es un Enum tipado y seguro!


    // Mapeo del Enum como String en Oracle. VARCHAR2(15) es ideal para soportar 'EN_SUSPENSION' (13 caracteres)
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_ETAPA", columnDefinition = "VARCHAR2(15) DEFAULT 'EN_PROGRESO'", nullable = false)
    private EstadoEtapa estadoEtapa;

    @Column(name = "NOMBRE_JEFE_INMEDIATO", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String nombreJefeInmediato;

    @Column(name = "CORREO_JEFE_INMEDIATO", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String correoJefeInmediato;

    @Column(name = "TELEFONO_JEFE_INMEDIATO", columnDefinition = "VARCHAR2(20)", nullable = false)
    private String telefonoJefeInmediato;

    // El Instructor de Seguimiento asignado a este aprendiz ya NO vive aquí como FK directo:
    // ver la tabla ASIGNACION_INSTRUCTOR_ETAPA (entidad AsignacionInstructorEtapa), que permite
    // trazabilidad histórica (reasignaciones) en vez de sobrescribir un único valor.

    // Setea el estado inicial por defecto en la capa de persistencia de Java
    @PrePersist
    protected void onPrePersist() {
        if (this.estadoEtapa == null) {
            this.estadoEtapa = EstadoEtapa.EN_PROGRESO;
        }
    }
}