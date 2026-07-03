package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "ROL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle 12c+
    @Column(name = "ID_ROL", columnDefinition = "NUMBER(19,0)")
    private Long idRol;

    @Column(name = "NOMBRE_ROL", columnDefinition = "VARCHAR2(40)", nullable = false, unique = true)
    private String nombreRol;
}