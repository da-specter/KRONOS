package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "USUARIO_ROL", uniqueConstraints = {
    // Candado de 5FN: Evita que en la base de datos se le asigne el mismo rol al mismo usuario dos veces
    @UniqueConstraint(columnNames = {"ID_USUARIO", "ID_ROL"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_USUARIO_ROL", columnDefinition = "NUMBER(19,0)")
    private Long idUsuarioRol;

    // Relación Muchos a Opciones: Muchos registros de asignación pueden pertenecer al mismo Usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Usuario usuario;

    // Relación Muchos a Opciones: Muchos registros de asignación pueden apuntar al mismo Rol
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ROL", referencedColumnName = "ID_ROL", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Rol rol;

}