package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "EMPRESA", uniqueConstraints = {
    // Candado de Integridad: No pueden existir dos empresas con el mismo NIT
    @UniqueConstraint(columnNames = "NIT")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_EMPRESA", columnDefinition = "NUMBER(19,0)")
    private Long idEmpresa;

    @Column(name = "NIT", columnDefinition = "VARCHAR2(20)", nullable = false)
    private String nit;

    @Column(name = "NOMBRE_EMPRESA", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String nombreEmpresa;

    @Column(name = "DIRECCION", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String direccion;

    @Column(name = "TELEFONO", columnDefinition = "VARCHAR2(20)")
    private String telefono;

    @Column(name = "CORREO", columnDefinition = "VARCHAR2(50)")
    private String correo;

    // Control de estado booleano mapeado como NUMBER(1,0) para Oracle DB
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // Relación Muchos a Uno: Muchas empresas pueden estar ubicadas en el mismo Municipio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_MUNICIPIO", 
        referencedColumnName = "ID_MUNICIPIO", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Municipio municipio;

    // Inicialización automática del estado en Activo (true) antes de registrar en Oracle
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = true;
        }
    }
}