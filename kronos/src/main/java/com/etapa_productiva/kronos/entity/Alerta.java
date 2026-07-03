package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "ALERTA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ALERTA", columnDefinition = "NUMBER(19,0)")
    private Long idAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_BITACORA", referencedColumnName = "ID_BITACORA", columnDefinition = "NUMBER(19,0)", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Bitacora bitacora;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_ALERTA", columnDefinition = "VARCHAR2(20)", nullable = false)
    private TipoAlerta tipoAlerta;

    @Column(name = "DESCRIPCION", columnDefinition = "VARCHAR2(250)", nullable = false)
    private String descripcion;

    @Column(name = "FECHA_GENERACION", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaGeneracion;

    // BOOLEAN -> NUMBER(1,0) nativo en Oracle
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaGeneracion == null) this.fechaGeneracion = LocalDateTime.now();
        if (this.estado == null) this.estado = true;
    }
}