package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ✍️ Datos de uno de los 3 "momentos" del Formato 023 (cada momento se habilita cuando las 4
 * bitácoras de su grupo quedan ENTREGADA: 1-4 → Momento 1, 5-8 → Momento 2, 9-12 → Momento 3).
 * Aprendiz e instructor escriben su parte en paralelo sobre la misma fila (quien llega primero la
 * crea, el otro completa las columnas que le corresponden). Al completarse los 3 momentos por
 * ambos lados, KRONOS genera el PDF del 023 inyectando cada dato en su sección correspondiente.
 */
@Entity
@Table(name = "EVALUACION_MOMENTO", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ID_ETAPA", "NUMERO_MOMENTO"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionMomento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_EVALUACION_MOMENTO", columnDefinition = "NUMBER(19,0)")
    private Long idEvaluacionMomento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_ETAPA",
        referencedColumnName = "ID_ETAPA",
        columnDefinition = "NUMBER(19,0)",
        nullable = false
    )
    private EtapaProductiva etapaProductiva;

    // 1, 2 o 3 — a qué momento del Formato 023 pertenece esta fila
    @Column(name = "NUMERO_MOMENTO", columnDefinition = "NUMBER(1,0)", nullable = false)
    private Integer numeroMomento;

    // ─────────────────────────── Lado Instructor de Seguimiento ───────────────────────────

    // Texto que escribe el instructor. Nullable: la fila puede existir ya con solo el lado del
    // aprendiz diligenciado (escriben en paralelo).
    @Column(name = "OBSERVACION", columnDefinition = "VARCHAR2(2000)")
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_INSTRUCTOR",
        referencedColumnName = "ID_USUARIO",
        columnDefinition = "NUMBER(19,0)"
    )
    private Usuario instructor;

    // Solo aplica al Momento 3: veredicto final que da el instructor
    @Enumerated(EnumType.STRING)
    @Column(name = "JUICIO_EVALUACION", columnDefinition = "VARCHAR2(15)")
    private JuicioEvaluacion juicioEvaluacion;

    // ────────────────────────────────── Lado Aprendiz ──────────────────────────────────

    // Texto propio del aprendiz (Momento 2/3: "Observaciones del aprendiz" / retroalimentación)
    @Column(name = "OBSERVACION_APRENDIZ", columnDefinition = "VARCHAR2(2000)")
    private String observacionAprendiz;

    // Solo Momento 1 ("Concertación plan de trabajo")
    @Column(name = "COMPETENCIAS_DESARROLLAR", columnDefinition = "VARCHAR2(2000)")
    private String competenciasDesarrollar;

    @Column(name = "RESULTADOS_APRENDIZAJE", columnDefinition = "VARCHAR2(2000)")
    private String resultadosAprendizaje;

    @Column(name = "ACTIVIDADES_DESARROLLAR", columnDefinition = "VARCHAR2(2000)")
    private String actividadesDesarrollar;

    @Column(name = "EVIDENCIA_DESCRIPCION", columnDefinition = "VARCHAR2(1000)")
    private String evidenciaDescripcion;

    @Column(name = "EVIDENCIA_RUTA_ARCHIVO", columnDefinition = "VARCHAR2(255)")
    private String evidenciaRutaArchivo;

    // Solo Momento 2: "Fecha del momento de seguimiento" (distinta de la fecha de diligenciamiento/firma)
    @Column(name = "FECHA_MOMENTO", columnDefinition = "DATE")
    private LocalDate fechaMomento;

    // Solo Momento 2, opcional: la transcribe el aprendiz porque no existe un rol/login de ente co-formador
    @Column(name = "OBSERVACION_ENTE_COFORMADOR", columnDefinition = "VARCHAR2(2000)")
    private String observacionEnteCoformador;

    // Solo Momento 3: retroalimentación dividida en "proceso de formación" y "desempeño de competencias"
    // por cada rol, en vez del campo genérico único que usan los Momentos 1 y 2.
    @Column(name = "RETRO_ENTE_PROCESO", columnDefinition = "VARCHAR2(2000)")
    private String retroEnteProceso;

    @Column(name = "RETRO_ENTE_DESEMPENO", columnDefinition = "VARCHAR2(2000)")
    private String retroEnteDesempeno;

    @Column(name = "RETRO_INSTRUCTOR_PROCESO", columnDefinition = "VARCHAR2(2000)")
    private String retroInstructorProceso;

    @Column(name = "RETRO_INSTRUCTOR_DESEMPENO", columnDefinition = "VARCHAR2(2000)")
    private String retroInstructorDesempeno;

    @Column(name = "RETRO_APRENDIZ_PROCESO", columnDefinition = "VARCHAR2(2000)")
    private String retroAprendizProceso;

    @Column(name = "RETRO_APRENDIZ_DESEMPENO", columnDefinition = "VARCHAR2(2000)")
    private String retroAprendizDesempeno;

    // ─────────────────────────────── Comunes a los 3 momentos ───────────────────────────────

    @Column(name = "ENLACE_GRABACION", columnDefinition = "VARCHAR2(500)")
    private String enlaceGrabacion;

    @Column(name = "CIUDAD", columnDefinition = "VARCHAR2(100)")
    private String ciudad;

    @Column(name = "FECHA_DILIGENCIAMIENTO", columnDefinition = "DATE")
    private LocalDate fechaDiligenciamiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODALIDAD_FIRMA", columnDefinition = "VARCHAR2(15) DEFAULT 'VIRTUAL'")
    private ModalidadFirma modalidadFirma;

    @Column(name = "FECHA_REGISTRO", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.modalidadFirma == null) {
            this.modalidadFirma = ModalidadFirma.VIRTUAL;
        }
    }
}
