package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ⚙️ Variables del sistema ajustables "en caliente" desde el módulo Configuración Global
 * del Administrador (Ej: días de anticipación de las alertas de visitas, modo mantenimiento).
 * Cada registro es un par CLAVE → VALOR que los servicios leen directo de Oracle en cada uso,
 * por lo que el cambio aplica de inmediato sin reiniciar la aplicación.
 */
@Entity
@Table(name = "CONFIGURACION_GLOBAL", uniqueConstraints = {
    @UniqueConstraint(columnNames = "CLAVE")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_CONFIGURACION", columnDefinition = "NUMBER(19,0)")
    private Long idConfiguracion;

    @Column(name = "CLAVE", columnDefinition = "VARCHAR2(60)", nullable = false)
    private String clave;

    @Column(name = "VALOR", columnDefinition = "VARCHAR2(250)", nullable = false)
    private String valor;

    @Column(name = "DESCRIPCION", columnDefinition = "VARCHAR2(250)")
    private String descripcion;

    @Column(name = "FECHA_ACTUALIZACION", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
