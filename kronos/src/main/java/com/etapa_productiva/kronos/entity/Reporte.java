package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "REPORTE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REPORTE", columnDefinition = "NUMBER(19,0)")
    private Long idReporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_REPORTE", columnDefinition = "VARCHAR2(20)", nullable = false)
    private TipoReporte tipoReporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "FORMATO", columnDefinition = "VARCHAR2(10)", nullable = false)
    private FormatoReporte formato;

    @Column(name = "FECHA_GENERACION", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "RUTA_ARCHIVO", columnDefinition = "VARCHAR2(500)", nullable = false)
    private String rutaArchivo;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaGeneracion == null) this.fechaGeneracion = LocalDateTime.now();
    }
}