package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICACIONES")
@Data
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_NOTIFICACION", columnDefinition = "NUMBER(19,0)")
    private Long idNotificacion;

    // A quién va dirigida la alerta (Instructor, Aprendiz o Coordinador)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO_DESTINO", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Usuario usuarioDestino;

    // El mensaje que se pintará en la campana de notificaciones (ej: "Tu bitácora #3 fue reprobada")
    @Column(name = "MENSAJE", columnDefinition = "VARCHAR2(255)", nullable = false)
    private String mensaje;

    // Para saber si el usuario ya le dio clic en la interfaz
    @Column(name = "LEIDO", columnDefinition = "NUMBER(1,0) DEFAULT 0", nullable = false)
    private Boolean leido;

    @Column(name = "FECHA_CREACION", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onPrePersist() {
        this.fechaCreacion = LocalDateTime.now();
        this.leido = false;
    }
}