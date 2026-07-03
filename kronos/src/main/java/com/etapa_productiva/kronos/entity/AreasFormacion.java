package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "AREAS_FORMACION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreasFormacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_AREA_FORMACION", columnDefinition = "NUMBER(19,0)")
    private Long idAreaFormacion;

    @Column(name = "NOMBRE_AREA_FORMACION", columnDefinition = "VARCHAR2(50)", nullable = false)
    private String nombreAreaFormacion;

    // Mapeo del booleano como NUMBER(1,0) para compatibilidad con Oracle DB
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // Relación Muchos a Uno: Muchas áreas de formación pueden pertenecer a una misma Coordinación Académica
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_COORDINACION", 
        referencedColumnName = "ID_COORDINACION", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private CoordinacionAcademica coordinacionAcademica;

    // Inicialización automática del estado en Activo (true) antes de guardar en Oracle
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = true;
        }
    }
}