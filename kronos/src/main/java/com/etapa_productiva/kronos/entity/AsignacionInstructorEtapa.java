package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 👥 Traza la asignación de un Instructor de Seguimiento a un aprendiz puntual
 * (su Etapa Productiva), permitiendo historial de reasignaciones en vez de
 * sobrescribir un único valor: solo debe existir una fila con ESTADO_ASIGNACION = 1 (Activo)
 * por Etapa Productiva a la vez (se controla en KronosWorkflowService).
 */
@Entity
@Table(name = "ASIGNACION_INSTRUCTOR_ETAPA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionInstructorEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ASIGNACION", columnDefinition = "NUMBER(19,0)")
    private Long idAsignacion;

    // Relación Muchos a Uno: el aprendiz (a través de su Etapa Productiva) que recibe el seguimiento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_ETAPA_PRODUCTIVA",
        referencedColumnName = "ID_ETAPA",
        columnDefinition = "NUMBER(19,0)",
        nullable = false
    )
    private EtapaProductiva etapaProductiva;

    // Relación Muchos a Uno: el Instructor de Seguimiento asignado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_INSTRUCTOR",
        referencedColumnName = "ID_INSTRUCTOR_SEGUIMIENTO",
        columnDefinition = "NUMBER(19,0)",
        nullable = false
    )
    private InstructorSeguimiento instructor;

    // Momento exacto en que el Gestor de Etapa realizó la asignación
    @Column(name = "FECHA_ASIGNACION", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaAsignacion;

    // Activo (1) = seguimiento vigente; Inactivo (0) = histórico, reemplazado por una reasignación
    @Column(name = "ESTADO_ASIGNACION", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estadoAsignacion;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaAsignacion == null) this.fechaAsignacion = LocalDateTime.now();
        if (this.estadoAsignacion == null) this.estadoAsignacion = true;
    }
}
