package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "APRENDIZ_FICHA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprendizFicha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_APRENDIZ_FICHA", columnDefinition = "NUMBER(19,0)")
    private Long idAprendizFicha;

    // Relación Uno a Uno con Usuario: Un usuario base con rol de aprendiz solo puede estar asociado 
    // a un registro de matrícula activo en el sistema. unique = true genera el candado en Oracle.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_USUARIO", 
        referencedColumnName = "ID_USUARIO", 
        columnDefinition = "NUMBER(19,0)", 
        unique = true, 
        nullable = false
    )
    private Usuario usuario;


    // EL CAMBIO CLAVE: Reemplazamos el Boolean por el Enum de estados académicos
    @Enumerated(EnumType.STRING) // Guarda el texto 'INICIADO', 'POR_CERTIFICAR' o 'CERTIFICADO' en Oracle
    @Column(name = "ESTADO_ACADEMICO", columnDefinition = "VARCHAR2(15) DEFAULT 'INICIADO'", nullable = false)
    private EstadoAcademico estadoAcademico;

    // Relación Muchos a Uno: Muchas matrículas de aprendices pertenecen a una misma Ficha académica.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_FICHA", 
        referencedColumnName = "ID_FICHA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Ficha ficha;


    // Interceptor automático: Setea el estado como INICIADO justo antes del INSERT en Oracle
    @PrePersist
    protected void onPrePersist() {
        if (this.estadoAcademico == null) {
            this.estadoAcademico = EstadoAcademico.INICIADO;
        }
    }
}