package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Entity
@Table(name = "EVALUACION_BITACORA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionBitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_EVALUACION_BITACORA", columnDefinition = "NUMBER(19,0)")
    private Long idEvaluacionBitacora;

    // Relación Muchos a Uno: Muchas evaluaciones pueden pertenecer a una misma Bitácora subida
    // Permite evaluar corregidos si el aprendiz vuelve a subir el archivo para el mismo cronograma
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_BITACORA", 
        referencedColumnName = "ID_BITACORA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Bitacora bitacora;

    // Relación Muchos a Uno: El usuario (Instructor) que realiza la calificación de la bitácora
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_INSTRUCTOR", 
        referencedColumnName = "ID_USUARIO", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Usuario instructor;

    // Reutilizamos el Enum global de resultados para evitar redundancia de archivos
    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", columnDefinition = "VARCHAR2(20)", nullable = false)
    private ResultadoEvaluacion resultado;

    // Espacio para la retroalimentación técnica o las correcciones exigidas por el instructor
    @Column(name = "OBSERVACIONES", columnDefinition = "VARCHAR2(500)")
    private String observaciones;

    // Fecha exacta en la que el instructor asienta la nota en KRONOS
    @Column(name = "FECHA_EVALUACION", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaEvaluacion;

    // Interceptor automático: Registra la fecha del día de hoy en Oracle antes de hacer el INSERT
    @PrePersist
    protected void onPrePersist() {
        if (this.fechaEvaluacion == null) {
            this.fechaEvaluacion = LocalDate.now();
        }
    }
}