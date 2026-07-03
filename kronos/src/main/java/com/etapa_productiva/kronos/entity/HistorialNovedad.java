package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORIAL_NOVEDAD")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialNovedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_HISTORIAL", columnDefinition = "NUMBER(19,0)")
    private Long idHistorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_NOVEDAD", referencedColumnName = "ID_NOVEDAD", columnDefinition = "NUMBER(19,0)", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Ejecuta de forma nativa el ON DELETE CASCADE en Oracle
    private Novedad novedad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO_ACCION", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)", nullable = false)
    private Usuario usuarioAccion;

    @Lob // TEXT -> CLOB
    @Column(name = "COMENTARIO_RESPUESTA", columnDefinition = "CLOB", nullable = false)
    private String comentarioRespuesta;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCION_REALIZADA", columnDefinition = "VARCHAR2(20)", nullable = false)
    private AccionRealizada accionRealizada;

    @Column(name = "FECHA_ACCION", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaAccion;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaAccion == null) this.fechaAccion = LocalDateTime.now();
    }
}