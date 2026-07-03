package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "COORDINACION_ACADEMICA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinacionAcademica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_COORDINACION", columnDefinition = "NUMBER(19,0)")
    private Long idCoordinacion;

    // Relación Uno a Uno: Un registro de coordinación pertenece exclusivamente a un Usuario.
    // unique = true asegura en Oracle que un usuario no pueda ser asignado como coordinador de dos bloques distintos.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_USUARIO", 
        referencedColumnName = "ID_USUARIO", 
        columnDefinition = "NUMBER(19,0)", 
        unique = true, 
        nullable = false
    )
    private Usuario usuario;
}
