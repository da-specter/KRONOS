package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "DOCUMENTO_SOLICITUD", uniqueConstraints = {
    // Un aprendiz no puede resubir dos veces la misma plantilla para la misma solicitud
    @UniqueConstraint(columnNames = {"ID_SOLICITUD", "ID_PLANTILLA"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DOCUMENTO_SOLICITUD", columnDefinition = "NUMBER(19,0)")
    private Long idDocumentoSolicitud;

    // Relación Muchos a Uno: el documento firmado pertenece a la solicitud del aprendiz (antes de que exista la Etapa Productiva)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SOLICITUD", referencedColumnName = "ID_SOLICITUD", columnDefinition = "NUMBER(19,0)", nullable = false)
    private SolicitudEtapaPractica solicitud;

    // Relación Muchos a Uno: identifica cuál plantilla en blanco fue la que diligenció el aprendiz
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PLANTILLA", referencedColumnName = "ID_PLANTILLA", columnDefinition = "NUMBER(19,0)", nullable = false)
    private PlantillaFormato plantillaFormato;

    // Almacena la ubicación física del archivo firmado en el servidor
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
