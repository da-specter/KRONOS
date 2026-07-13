package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ETAPA_PRODUCTIVA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaProductiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // GENERATED AS IDENTITY nativo de Oracle 12c+
    @Column(name = "ID_ETAPA", columnDefinition = "NUMBER(19,0)")
    private Long idEtapa;

    // Relación Muchos a Uno: Conecta con la matrícula del aprendiz en la ficha (5FN)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_APRENDIZ_FICHA", 
        referencedColumnName = "ID_APRENDIZ_FICHA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private AprendizFicha aprendizFicha;

    // Relación Muchos a Uno: Conecta con la empresa coformadora
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_EMPRESA", 
        referencedColumnName = "ID_EMPRESA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private Empresa empresa;

    // Relación Muchos a Uno: Conecta con la modalidad (Contrato de aprendizaje, pasantía, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_TIPO_CONTRATO", 
        referencedColumnName = "ID_TIPO_CONTRATO", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private TipoContrato tipoContrato;

    // Momento real en que Registro confirmó el registro (auditoría del sistema). No confundir
    // con FECHA_INICIO: esa es la fecha contractual que Registro digita a mano y puede ser
    // pasada o futura. Nullable porque las etapas creadas antes de este campo no lo tienen.
    @Column(name = "FECHA_CREACION", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaCreacion;

    @Column(name = "FECHA_INICIO", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "FECHA_FIN", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODALIDAD", columnDefinition = "VARCHAR2(20)", nullable = false)
    private ModalidadEtapa modalidad; // 🚀 ¡Ahora es un Enum tipado y seguro!


    // Mapeo del Enum como String en Oracle. VARCHAR2(15) es ideal para soportar 'EN_SUSPENSION' (13 caracteres)
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_ETAPA", columnDefinition = "VARCHAR2(15) DEFAULT 'EN_PROGRESO'", nullable = false)
    private EstadoEtapa estadoEtapa;

    // Momento exacto en que la etapa pasó a TERMINADO (100% de bitácoras + Formato 023
    // aprobados). No es una certificación: la certificación oficial ya no la hace el Gestor
    // de Etapa manualmente, ocurre en otra plataforma (Sofía Plus). KRONOS solo refleja aquí
    // que su ciclo interno quedó cerrado (ver EvaluacionFormatosService.verificarYTransicionarPorCertificar).
    @Column(name = "FECHA_TERMINACION", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaTerminacion;

    @Column(name = "NOMBRE_JEFE_INMEDIATO", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String nombreJefeInmediato;

    @Column(name = "CORREO_JEFE_INMEDIATO", columnDefinition = "VARCHAR2(100)", nullable = false)
    private String correoJefeInmediato;

    @Column(name = "TELEFONO_JEFE_INMEDIATO", columnDefinition = "VARCHAR2(20)", nullable = false)
    private String telefonoJefeInmediato;

    // Trazabilidad de qué usuario del rol REGISTRO realizó este registro. Nullable porque las
    // etapas creadas antes de separar el rol REGISTRO del GESTOR_ETAPA no tienen este dato.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_USUARIO_REGISTRO",
        referencedColumnName = "ID_USUARIO",
        columnDefinition = "NUMBER(19,0)"
    )
    private Usuario usuarioRegistro;

    // El Instructor de Seguimiento asignado a este aprendiz ya NO vive aquí como FK directo:
    // ver la tabla ASIGNACION_INSTRUCTOR_ETAPA (entidad AsignacionInstructorEtapa), que permite
    // trazabilidad histórica (reasignaciones) en vez de sobrescribir un único valor.

    // Bandera de dedup del job diario de alertas: se enciende una sola vez, cuando ya pasaron
    // los días configurados (15 por defecto) desde FECHA_INICIO sin que exista ninguna visita
    // de seguimiento agendada, para avisarle al Instructor de Seguimiento que debe agendarla.
    @Column(name = "ALERTA_PRIMERA_VISITA_ENVIADA", columnDefinition = "NUMBER(1,0) DEFAULT 0", nullable = false)
    private Boolean alertaPrimeraVisitaEnviada;

    // Correo institucional que el aprendiz digita en el Formato 023 (distinto de USUARIO.CORREO,
    // que es la credencial de acceso) — lo pide el formato GFPI-F-023 real de SENA.
    @Column(name = "CORREO_INSTITUCIONAL_APRENDIZ", columnDefinition = "VARCHAR2(150)")
    private String correoInstitucionalAprendiz;

    // Firmas del Formato 023 como imagen: se capturan una sola vez por Etapa Productiva y se
    // reutilizan en las 3 secciones de firma del PDF generado (mismo patrón que USUARIO.FOTO_PERFIL).
    @Column(name = "FIRMA_APRENDIZ_RUTA", columnDefinition = "VARCHAR2(255)")
    private String firmaAprendizRuta;

    @Column(name = "FIRMA_INSTRUCTOR_RUTA", columnDefinition = "VARCHAR2(255)")
    private String firmaInstructorRuta;

    // La sube el aprendiz (no hay un actor "empresa" logueado en KRONOS)
    @Column(name = "FIRMA_ENTE_COFORMADOR_RUTA", columnDefinition = "VARCHAR2(255)")
    private String firmaEnteCoformadorRuta;

    // Setea el estado inicial por defecto en la capa de persistencia de Java
    @PrePersist
    protected void onPrePersist() {
        if (this.estadoEtapa == null) {
            this.estadoEtapa = EstadoEtapa.EN_PROGRESO;
        }
        if (this.alertaPrimeraVisitaEnviada == null) {
            this.alertaPrimeraVisitaEnviada = false;
        }
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}