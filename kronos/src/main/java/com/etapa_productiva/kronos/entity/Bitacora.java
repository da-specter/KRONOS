package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "BITACORA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_BITACORA", columnDefinition = "NUMBER(19,0)")
    private Long idBitacora;

    // Relación Muchos a Uno: Muchas bitácoras (intentos/versiones) pueden subirse para un mismo cupo del Cronograma
    // Esto respeta el símbolo de la pata de gallo (1 a muchos) que dibujaste desde CRONOGRAMA_BITACORAS
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_CRONOGRAMA", 
        referencedColumnName = "ID_CRONOGRAMA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private CronogramaBitacoras cronogramaBitacora;

    // Almacena la ruta física o URL del PDF de la bitácora en tu servidor de archivos
    @Column(name = "RUTA_ARCHIVO", columnDefinition = "VARCHAR2(255)", nullable = false)
    private String rutaArchivo;

    // Asunto que el aprendiz escribe al radicar la bitácora
    @Column(name = "ASUNTO", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String asunto;

    // Fecha exacta en la que el aprendiz cargó el archivo al sistema
    @Column(name = "FECHA_ENTREGA", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaEntrega;

    // Fecha y hora exactas de la carga (además de FECHA_ENTREGA, que solo guarda el día)
    @Column(name = "FECHA_HORA_SUBIDA", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaHoraSubida;

    // Interceptor automático: Setea la fecha de entrega con el día actual justo antes del INSERT en Oracle
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