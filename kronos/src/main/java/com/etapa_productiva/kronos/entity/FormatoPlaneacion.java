package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "FORMATO_PLANEACION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormatoPlaneacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // GENERATED AS IDENTITY nativo de Oracle
    @Column(name = "ID_FORMATO_PLANEACION", columnDefinition = "NUMBER(19,0)")
    private Long idFormatoPlaneacion;

    // Relación Muchos a Uno: Un formato de planeación pertenece a una Etapa Productiva específica
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_ETAPA", 
        referencedColumnName = "ID_ETAPA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private EtapaProductiva etapaProductiva;

    // Almacena la ruta o URL del archivo PDF/Word en el servidor
    @Column(name = "RUTA_ARCHIVO", columnDefinition = "VARCHAR2(255)", nullable = false)
    private String rutaArchivo;

    // Asunto que el aprendiz escribe al radicar el formato de planeación
    @Column(name = "ASUNTO", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String asunto;

    // Fecha en la que el aprendiz carga el documento en KRONOS
    @Column(name = "FECHA_ENTREGA", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaEntrega;

    // Fecha y hora exactas de la carga (además de FECHA_ENTREGA, que solo guarda el día)
    @Column(name = "FECHA_HORA_SUBIDA", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaHoraSubida;

    // Interceptor automático: Setea la fecha de entrega con el día actual justo antes del INSERT
    @PrePersist
    protected void onPrePersist() {
        if (this.fechaEntrega == null) {
            this.fechaEntrega = LocalDate.now();
        }
        if (this.fechaHoraSubida == null) {
            this.fechaHoraSubida = LocalDateTime.now();
        }
    }
}