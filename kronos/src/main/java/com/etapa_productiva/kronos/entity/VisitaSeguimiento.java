package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Entity
@Table(name = "VISITA_SEGUIMIENTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitaSeguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_VISITA", columnDefinition = "NUMBER(19,0)")
    private Long idVisita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ETAPA", referencedColumnName = "ID_ETAPA", columnDefinition = "NUMBER(19,0)", nullable = false)
    private EtapaProductiva etapaProductiva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_INSTRUCTOR", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Usuario instructor;

    @Column(name = "FECHA_VISITA", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaVisita;

    // Sincronizado con 'ACTA VISITA' de tu Excel
    @Column(name = "NUMERO_ACTA", columnDefinition = "VARCHAR2(50)")
    private String numeroActa;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODALIDAD", columnDefinition = "VARCHAR2(20)", nullable = false)
    private ModalidadVisita modalidad;

    // NUEVO CAMPO: Sincronizado con 'TIPO DE VISITA' de tu Excel
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_VISITA", columnDefinition = "VARCHAR2(20)", nullable = false)
    private TipoVisita tipoVisita;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_VISITA", columnDefinition = "VARCHAR2(15) DEFAULT 'PLANEADA'", nullable = false)
    private EstadoVisita estadoVisita;

    @Column(name = "OBSERVACIONES", columnDefinition = "VARCHAR2(500)")
    private String observaciones;

    @PrePersist
    protected void onPrePersist() {
        if (this.estadoVisita == null) {
            this.estadoVisita = EstadoVisita.PLANEADA;
        }
    }
}