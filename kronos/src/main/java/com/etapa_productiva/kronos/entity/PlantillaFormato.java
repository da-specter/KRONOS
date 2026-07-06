package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PLANTILLA_FORMATO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaFormato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PLANTILLA", columnDefinition = "NUMBER(19,0)")
    private Long idPlantilla;

    // Relación con las 4 alternativas. 
    // Si la visibilidad es 'SOLO_APRENDIZ' (F023/Bitácora), este campo se guarda como NULL en Oracle.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SECCION_FORMATO", referencedColumnName = "ID_SECCION_FORMATO", columnDefinition = "NUMBER(19,0)", nullable = true)
    private SeccionFormato seccionFormato;

    @Column(name = "NOMBRE_DOCUMENTO", columnDefinition = "VARCHAR2(150)", nullable = false)
    private String nombreDocumento;

    @Column(name = "RUTA_ARCHIVO_PLANTILLA", columnDefinition = "VARCHAR2(500)", nullable = false)
    private String rutaArchivoPlantilla;

    // CONTROL DE ACCESO: Guarda 'SOLO_APRENDIZ' o 'SOLO_COORDINADOR'
    @Enumerated(EnumType.STRING)
    @Column(name = "VISIBILIDAD", columnDefinition = "VARCHAR2(20)", nullable = false)
    private VisibilidadDocumento visibilidad;

    @Column(name = "FECHA_SUBIDA", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    @PrePersist
    protected void onPrePersist() {
        if (this.fechaSubida == null) this.fechaSubida = LocalDateTime.now();
        if (this.estado == null) this.estado = true;
    }

    // El archivo físico se guarda como "{timestamp}_{nombreOriginal}" (ver AdminPlantillasController.guardarArchivo).
    // Esto expone ese nombre original tal cual se llama el archivo, sin el prefijo de timestamp ni la carpeta.
    public String getNombreArchivoOriginal() {
        if (rutaArchivoPlantilla == null) return "";
        String base = rutaArchivoPlantilla.substring(rutaArchivoPlantilla.lastIndexOf('/') + 1);
        int guionBajo = base.indexOf('_');
        return (guionBajo > 0 && guionBajo < base.length() - 1) ? base.substring(guionBajo + 1) : base;
    }
}