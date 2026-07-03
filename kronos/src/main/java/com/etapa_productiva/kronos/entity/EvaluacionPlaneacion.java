package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Entity
@Table(name = "EVALUACION_PLANEACION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionPlaneacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_EVALUACION_PLANEACION", columnDefinition = "NUMBER(19,0)")
    private Long idEvaluacionPlaneacion;

    // Relación Muchos a Uno: Muchas evaluaciones pueden pertenecer a un mismo Formato
    // (Por si el aprendiz corrige y el instructor vuelve a evaluar el mismo formato)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_FORMATO_PLANEACION", 
        referencedColumnName = "ID_FORMATO_PLANEACION", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private FormatoPlaneacion formatoPlaneacion;

    // Relación Muchos a Uno: El usuario (con rol Instructor) que realiza la calificación
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_INSTRUCTOR", 
        referencedColumnName = "ID_USUARIO", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Usuario instructor;

    // AQUÍ VA EL ENUM: Registra el veredicto del instructor en Oracle como texto
    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", columnDefinition = "VARCHAR2(20)", nullable = false)
    private ResultadoEvaluacion resultado;

    // Campo amplio para que el instructor escriba qué debe corregir el aprendiz si reprueba
    @Column(name = "OBSERVACIONES", columnDefinition = "VARCHAR2(500)")
    private String observaciones;

    // Fecha en la que el instructor guardó la revisión
    @Column(name = "FECHA_EVALUACION", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaEvaluacion;

    // Interceptor automático: Setea la fecha de evaluación con el día actual justo antes del INSERT
    @PrePersist
    protected void onPrePersist() {
        if (this.fechaEvaluacion == null) {
            this.fechaEvaluacion = LocalDate.now();
        }
    }
}