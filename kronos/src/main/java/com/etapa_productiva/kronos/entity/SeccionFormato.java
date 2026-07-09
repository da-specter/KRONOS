package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SECCION_FORMATO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeccionFormato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_SECCION_FORMATO", columnDefinition = "NUMBER(19,0)")
    private Long idSeccionFormato;

    // Almacenará: "PASANTIA", "MONITORIA", "PROYECTO PRODUCTIVO", "VINCULACION LABORAL"
    @Column(name = "NOMBRE_SECCION", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String nombreSeccion;

    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) this.estado = true;
    }

    // 💼 La modalidad Vinculación Laboral tiene reglas propias en el flujo de Etapa Práctica
    // (habilitación 3 meses más temprana, y el check de competencias no la bloquea).
    @Transient
    public boolean esVinculacionLaboral() {
        return "VINCULACION LABORAL".equalsIgnoreCase(nombreSeccion);
    }
}