package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 📋 Una de las 13 variables (8 Factores Técnicos + 5 Actitudinales) que trae el Momento 2 y el
 * Momento 3 del Formato 023 real de SENA. Se siembra automáticamente (valoración/observación en
 * blanco) la primera vez que se necesita un `EvaluacionMomento` de número 2 o 3.
 */
@Entity
@Table(name = "FACTOR_MOMENTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactorMomento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_FACTOR", columnDefinition = "NUMBER(19,0)")
    private Long idFactor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_EVALUACION_MOMENTO",
        referencedColumnName = "ID_EVALUACION_MOMENTO",
        columnDefinition = "NUMBER(19,0)",
        nullable = false
    )
    private EvaluacionMomento evaluacionMomento;

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORIA", columnDefinition = "VARCHAR2(15)", nullable = false)
    private CategoriaFactor categoria;

    @Column(name = "NOMBRE_VARIABLE", columnDefinition = "VARCHAR2(60)", nullable = false)
    private String nombreVariable;

    // Sin marcar hasta que exista una UI para calificar factor por factor (fase futura)
    @Enumerated(EnumType.STRING)
    @Column(name = "VALORACION", columnDefinition = "VARCHAR2(15)")
    private ValoracionFactor valoracion;

    @Column(name = "OBSERVACION", columnDefinition = "VARCHAR2(500)")
    private String observacion;
}
