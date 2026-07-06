package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 🔑 Código de un solo uso para el flujo "Olvidé mi contraseña" (login).
 * Se genera al solicitar recuperación por correo y se invalida al usarse o al vencer.
 */
@Entity
@Table(name = "CODIGO_RECUPERACION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CODIGO", columnDefinition = "NUMBER(19,0)")
    private Long idCodigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Usuario usuario;

    @Column(name = "CODIGO", columnDefinition = "VARCHAR2(6)", nullable = false)
    private String codigo;

    @Column(name = "FECHA_EXPIRACION", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "USADO", columnDefinition = "NUMBER(1,0) DEFAULT 0", nullable = false)
    private Boolean usado;

    @PrePersist
    protected void onPrePersist() {
        if (this.usado == null) {
            this.usado = false;
        }
    }
}
