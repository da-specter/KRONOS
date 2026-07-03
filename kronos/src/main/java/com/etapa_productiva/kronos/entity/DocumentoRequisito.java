package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "DOCUMENTO_REQUISITO", uniqueConstraints = {
    // Restricción única empresarial: Un aprendiz no puede subir dos veces el mismo documento para la misma etapa
    @UniqueConstraint(columnNames = {"ID_ETAPA", "ID_PLANTILLA"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoRequisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_DOCUMENTO_REQUISITO", columnDefinition = "NUMBER(19,0)")
    private Long idDocumentoRequisito;

    // Relación Muchos a Uno: El documento pertenece a la etapa productiva de un aprendiz específico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ETAPA", referencedColumnName = "ID_ETAPA", columnDefinition = "NUMBER(19,0)", nullable = false)
    private EtapaProductiva etapaProductiva;

    // Relación Muchos a Uno: Identifica cuál plantilla en blanco fue la que diligenció el aprendiz
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PLANTILLA", referencedColumnName = "ID_PLANTILLA", columnDefinition = "NUMBER(19,0)", nullable = false)
    private PlantillaFormato plantillaFormato;

    // Almacena la ubicación física del archivo en el File Server (Ej: "/storage/requisitos/contrato_1027.pdf")
    @Column(name = "RUTA_ARCHIVO_LLENO", columnDefinition = "VARCHAR2(500)", nullable = false)
    private String rutaArchivoLleno;

    // Estado individual de la revisión usando el Enum (PENDIENTE, APROBADO, RECHAZADO)
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_VALIDACION", columnDefinition = "VARCHAR2(20) DEFAULT 'PENDIENTE'", nullable = false)
    private EstadoValidacion estadoValidacion;

    // Estampa de tiempo exacta de la entrega
    @Column(name = "FECHA_SUBIDA", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaSubida;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaSubida == null) this.fechaSubida = LocalDateTime.now();
        if (this.estadoValidacion == null) this.estadoValidacion = EstadoValidacion.PENDIENTE;
    }
}