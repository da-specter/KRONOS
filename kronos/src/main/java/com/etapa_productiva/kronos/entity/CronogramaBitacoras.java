package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Entity
@Table(name = "CRONOGRAMA_BITACORAS", uniqueConstraints = {
    // Candado de Integridad: Evita que para una misma etapa se duplique el número de una bitácora
    @UniqueConstraint(columnNames = {"ID_ETAPA", "NUMERO_BITACORA"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CronogramaBitacoras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_CRONOGRAMA", columnDefinition = "NUMBER(19,0)")
    private Long idCronograma;

    // Relación Muchos a Uno: Una Etapa Productiva genera un bloque de 12 fechas en el cronograma
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_ETAPA", 
        referencedColumnName = "ID_ETAPA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private EtapaProductiva etapaProductiva;

    // Identificador secuencial de la entrega (ej: Bitácora 1, Bitácora 2, ..., Bitácora 12)
    @Column(name = "NUMERO_BITACORA", columnDefinition = "NUMBER(2,0)", nullable = false)
    private Integer numeroBitacora;

    // Fecha en la que el sistema abre el buzón para recibir el PDF
    @Column(name = "FECHA_APERTURA", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaApertura;

    // Fecha máxima de entrega sin que el sistema genere alertas de extemporaneidad
    @Column(name = "FECHA_LIMITE", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaLimite;

    // Estado de la entrega de este cupo: nace en PENDIENTE y pasa a ENTREGADA cuando el aprendiz radica su Bitácora
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", columnDefinition = "VARCHAR2(10) DEFAULT 'PENDIENTE'", nullable = false)
    private EstadoBitacora estado;

    // Interceptor automático: Setea el estado inicial como PENDIENTE justo antes del INSERT en Oracle
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = EstadoBitacora.PENDIENTE;
        }
    }
}