package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "INSTRUCTOR_TECNICO_FICHA", uniqueConstraints = {
    // Candado de Integridad: Evita asignar el mismo instructor a la misma ficha más de una vez de forma duplicada
    @UniqueConstraint(columnNames = {"ID_INSTRUCTOR_TECNICO", "ID_FICHA"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorTecnicoFicha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_INSTRUCTOR_FICHA", columnDefinition = "NUMBER(19,0)")
    private Long idInstructorFicha;

    // Relación Muchos a Uno: Muchas asignaciones pertenecen a un mismo Instructor Técnico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_INSTRUCTOR_TECNICO", 
        referencedColumnName = "ID_INSTRUCTOR_TECNICO", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private InstructorTecnico instructorTecnico;

    // Relación Muchos a Uno: Muchas asignaciones se vinculan a una misma Ficha
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_FICHA", 
        referencedColumnName = "ID_FICHA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Ficha ficha;

    // Control de estado para activación lógica de la asignación (ej: Activo = 1, Histórico/Inactivo = 0)
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // Interceptor automático: Asegura que la asignación nazca activa (true) antes del INSERT en Oracle
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = true;
        }
    }
}