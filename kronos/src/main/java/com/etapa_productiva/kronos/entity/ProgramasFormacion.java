package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "PROGRAMA_FORMACION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramasFormacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY en Oracle
    @Column(name = "ID_PROGRAMA", columnDefinition = "NUMBER(19,0)")
    private Long idPrograma;

    @Column(name = "NOMBRE_PROGRAMA", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String nombrePrograma;

    // Control de estado booleano mapeado como NUMBER(1,0) para Oracle DB
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // Relación Muchos a Uno: Muchos programas pertenecen a una misma Área de Formación
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_AREA_FORMACION", 
        referencedColumnName = "ID_AREA_FORMACION", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private AreasFormacion areaFormacion;

    // Relación Muchos a Uno: Muchos programas comparten el mismo Nivel de Formación (Técnico/Tecnólogo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_NIVEL", 
        referencedColumnName = "ID_NIVEL", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private NivelFormacion nivelFormacion;

    // Asegura que el programa se registre como Activo por defecto
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = true;
        }
    }
}