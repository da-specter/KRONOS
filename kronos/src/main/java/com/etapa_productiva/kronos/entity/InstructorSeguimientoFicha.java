package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "INSTRUCTOR_SEGUIMIENTO_FICHA", uniqueConstraints = {
    // Evita duplicar la asignación del mismo instructor a la misma ficha
    @UniqueConstraint(columnNames = {"ID_INSTRUCTOR_SEGUIMIENTO", "ID_FICHA"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorSeguimientoFicha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_INST_SEGUIMIENTO_FICHA", columnDefinition = "NUMBER(19,0)")
    private Long idInstructorSeguimientoFicha;

    // Relación Muchos a Uno: Vinculación con el perfil del Instructor de Seguimiento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_INSTRUCTOR_SEGUIMIENTO", 
        referencedColumnName = "ID_INSTRUCTOR_SEGUIMIENTO", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private InstructorSeguimiento instructorSeguimiento;

    // Relación Muchos a Uno: Vinculación con la Ficha académica mapeada
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_FICHA", 
        referencedColumnName = "ID_FICHA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Ficha ficha;

    // Registra el momento exacto en que la coordinación académica realiza la asignación
    @Column(name = "FECHA_ASIGNACION", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaAsignacion;

    // Control de borrado lógico institucional (1 = Asignación Activa, 0 = Histórico/Inactivo)
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaAsignacion == null) this.fechaAsignacion = LocalDateTime.now();
        if (this.estado == null) this.estado = true;
    }
}