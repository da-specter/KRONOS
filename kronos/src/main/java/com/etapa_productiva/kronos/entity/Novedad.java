package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOVEDAD")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Novedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_NOVEDAD", columnDefinition = "NUMBER(19,0)")
    private Long idNovedad;

    // Nullable: las novedades de tipo INFORMATIVO (chat GESTOR_ETAPA <-> REGISTRO) no están
    // atadas a ninguna Etapa Productiva puntual; las demás (radicadas por un aprendiz) sí.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ETAPA", referencedColumnName = "ID_ETAPA", columnDefinition = "NUMBER(19,0)")
    private EtapaProductiva etapaProductiva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_REMITENTE", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Usuario remitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DESTINATARIO_AC", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Usuario destinatarioAc;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_NOVEDAD", columnDefinition = "VARCHAR2(20)", nullable = false)
    private TipoNovedad tipoNovedad;

    @Lob // Mapea TEXT de forma nativa a CLOB en Oracle DB
    @Column(name = "DESCRIPCION", columnDefinition = "CLOB", nullable = false)
    private String descripcion;

    @Column(name = "URL_SOPORTE", columnDefinition = "VARCHAR2(500)")
    private String urlSoporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_FILTRO", columnDefinition = "VARCHAR2(25) DEFAULT 'SOLICITADO'", nullable = false)
    private EstadoFiltro estadoFiltro;

    @Column(name = "FECHA_CREACION", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaCreacion == null) this.fechaCreacion = LocalDateTime.now();
        if (this.estadoFiltro == null) this.estadoFiltro = EstadoFiltro.SOLICITADO;
    }
}