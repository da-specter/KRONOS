package com.etapa_productiva.kronos.entity; // Asegúrate de que use tu mismo paquete

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import com.etapa_productiva.kronos.entity.ModalidadEtapa; // Tu enum de modalidades
import com.etapa_productiva.kronos.entity.EstadoSolicitud;          // Tu enum de estados

@Entity
@Table(name = "SOLICITUD_ETAPA_PRACTICA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudEtapaPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SOLICITUD")
    private Long idSolicitud;

    // Relación Muchos a Uno: Conecta con la matrícula del aprendiz que radica la solicitud
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_FICHA_APRENDIZ",
        referencedColumnName = "ID_APRENDIZ_FICHA",
        columnDefinition = "NUMBER(19,0)",
        nullable = false
    )
    private AprendizFicha aprendizFicha;

    // Relación Muchos a Uno: Modalidad de contrato aspirada (Pasantía, Vínculo Laboral, Monitoría, Proyecto Productivo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_SECCION_FORMATO",
        referencedColumnName = "ID_SECCION_FORMATO",
        columnDefinition = "NUMBER(19,0)",
        nullable = false
    )
    private SeccionFormato seccionFormato;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODALIDAD_SOLICITADA", columnDefinition = "VARCHAR2(20)", nullable = false)
    private ModalidadEtapa modalidadSolicitada; // 🚀 Una sola definición limpia con el Enum

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", columnDefinition = "VARCHAR2(25)", nullable = false)
    private EstadoSolicitud estado;

    @Column(name = "CHECK_FECHA_ESTIPULADA", nullable = false)
    private boolean checkFechaEstipulada;

    @Column(name = "CHECK_COMPETENCIAS_APROBADAS", nullable = false)
    private boolean checkCompetenciasAprobadas;

    @Column(name = "CHECK_MODALIDAD_APROBADA", nullable = false)
    private boolean checkModalidadAprobada;

    @Column(name = "CHECK_FORMATOS_RADICADOS", nullable = false)
    private boolean checkFormatosRadicados;

    @Column(name = "RUTA_FORMATOS_SUBIDOS", columnDefinition = "VARCHAR2(255)")
    private String rutaFormatosSubidos;

    // Bandera independiente del ESTADO: la habilita el Gestor de Etapa para desbloquear
    // el panel de descarga/resubida de plantillas del aprendiz (no reemplaza el flujo del Coordinador).
    @Column(name = "PLANTILLAS_HABILITADAS", columnDefinition = "NUMBER(1,0) DEFAULT 0", nullable = false)
    private boolean plantillasHabilitadas;

    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private LocalDateTime fechaActualizacion;

    // Motivo que el Gestor de Etapa deja al rechazar un check (fecha/competencias/modalidad/formatos).
    // Se limpia automáticamente cuando el aprendiz reenvía la solicitud o cuando un check pasa a aprobado.
    @Lob
    @Column(name = "OBSERVACION_RECHAZO", columnDefinition = "CLOB")
    private String observacionRechazo;
}