package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ⏰ Historial de cada corrida del job automático de la 1:00 a.m. (VisitaAlertaService),
 * consultado desde el módulo "Monitoreo de Jobs" del Administrador para verificar si las
 * alertas de 2 y 3 días antes de las visitas se enviaron correctamente.
 */
@Entity
@Table(name = "JOB_EJECUCION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobEjecucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_JOB_EJECUCION", columnDefinition = "NUMBER(19,0)")
    private Long idJobEjecucion;

    @Column(name = "NOMBRE_JOB", columnDefinition = "VARCHAR2(80)", nullable = false)
    private String nombreJob;

    @Column(name = "FECHA_INICIO", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "FECHA_FIN", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaFin;

    // Mapeo del booleano como NUMBER(1,0) para compatibilidad con Oracle DB
    @Column(name = "EXITO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean exito;

    @Column(name = "ALERTAS_INSTRUCTORES", columnDefinition = "NUMBER(10,0)", nullable = false)
    private Integer alertasInstructores;

    @Column(name = "ALERTAS_APRENDICES", columnDefinition = "NUMBER(10,0)", nullable = false)
    private Integer alertasAprendices;

    @Column(name = "CORREOS_ENVIADOS", columnDefinition = "NUMBER(10,0)", nullable = false)
    private Integer correosEnviados;

    // Resumen legible de la corrida o el mensaje de error si el job falló
    @Column(name = "DETALLE", columnDefinition = "VARCHAR2(500)")
    private String detalle;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaInicio == null) this.fechaInicio = LocalDateTime.now();
        if (this.exito == null) this.exito = true;
        if (this.alertasInstructores == null) this.alertasInstructores = 0;
        if (this.alertasAprendices == null) this.alertasAprendices = 0;
        if (this.correosEnviados == null) this.correosEnviados = 0;
    }
}
