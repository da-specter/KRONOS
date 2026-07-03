package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "INSTRUCTOR_SEGUIMIENTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorSeguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY en Oracle
    @Column(name = "ID_INSTRUCTOR_SEGUIMIENTO", columnDefinition = "NUMBER(19,0)")
    private Long idInstructorSeguimiento;

    // Relación Uno a Uno: Un instructor de seguimiento es un Usuario único en el sistema
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_USUARIO", 
        referencedColumnName = "ID_USUARIO", 
        columnDefinition = "NUMBER(19,0)", 
        unique = true, 
        nullable = false
    )
    private Usuario usuario;

    // Relación Uno a Uno / Muchos a Uno Protegido: 
    // unique = true garantiza en Oracle que haya un único líder de seguimiento por Área de Formación
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_AREA_FORMACION", 
        referencedColumnName = "ID_AREA_FORMACION", 
        columnDefinition = "NUMBER(19,0)", 
        unique = true, 
        nullable = false
    )
    private AreasFormacion areaFormacion;
}
