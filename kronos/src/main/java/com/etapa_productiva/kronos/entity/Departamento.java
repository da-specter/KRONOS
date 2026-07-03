package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "DEPARTAMENTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_DEPARTAMENTO", columnDefinition = "NUMBER(19,0)")
    private Long idDepartamento;

    @Column(name = "NOMBRE_DEPARTAMENTO", columnDefinition = "VARCHAR2(50)", nullable = false, unique = true)
    private String nombreDepartamento;
}