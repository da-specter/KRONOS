package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "MUNICIPIO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_MUNICIPIO", columnDefinition = "NUMBER(19,0)")
    private Long idMunicipio;

    @Column(name = "NOMBRE_MUNICIPIO", columnDefinition = "VARCHAR2(50)", nullable = false)
    private String nombreMunicipio;

    // Relación Muchos a Uno: Muchos municipios pertenecen a un mismo Departamento (Ej: Apartadó y Carepa pertenecen a Antioquia)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_DEPARTAMENTO", 
        referencedColumnName = "ID_DEPARTAMENTO", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Departamento departamento;
}