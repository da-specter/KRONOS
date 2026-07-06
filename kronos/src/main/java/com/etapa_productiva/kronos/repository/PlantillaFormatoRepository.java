package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.PlantillaFormato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlantillaFormatoRepository extends JpaRepository<PlantillaFormato, Long> {

    // Cambiado a SQL Nativo para evitar conflictos con rutas y nombres de Enums en Java
    @Query(value = "SELECT p.* FROM plantilla_formato p WHERE p.estado = 1 " +
                   "AND p.visibilidad = 'SOLO_APRENDIZ' " +
                   "AND (p.id_seccion_formato = :idSeccion OR p.id_seccion_formato IS NULL) " +
                   "AND EXISTS (SELECT 1 FROM etapa_productiva e WHERE e.id_etapa = :idEtapa " +
                   "AND e.estado_etapa IN ('APROBADA', 'APROBADO', 'EN_PROCESO', 'EN_PROGRESO'))", 
           nativeQuery = true)
    List<PlantillaFormato> findFormatosHabilitadosParaAprendiz(
            @Param("idSeccion") Long idSeccion,
            @Param("idEtapa") Long idEtapa
    );

    // Plantillas de catálogo activas (Word/Excel/PDF), vinculadas estrictamente al tipo de contrato (SeccionFormato) de la solicitud
    @Query(value = "SELECT p.* FROM plantilla_formato p " +
                   "WHERE p.estado = 1 " +
                   "AND p.id_seccion_formato = :idSeccion " +
                   "AND (LOWER(p.ruta_archivo_plantilla) LIKE '%.doc' OR LOWER(p.ruta_archivo_plantilla) LIKE '%.docx' " +
                   "OR LOWER(p.ruta_archivo_plantilla) LIKE '%.xls' OR LOWER(p.ruta_archivo_plantilla) LIKE '%.xlsx' " +
                   "OR LOWER(p.ruta_archivo_plantilla) LIKE '%.pdf')",
           nativeQuery = true)
    List<PlantillaFormato> findWordExcelPorSeccion(@Param("idSeccion") Long idSeccion);

    // La plantilla que representa el requisito ARL (para colgar de ella los DocumentoRequisito subidos por el Gestor)
    java.util.Optional<PlantillaFormato> findFirstByNombreDocumentoContainingIgnoreCase(String nombreDocumento);

    // Plantillas generales de Etapa Práctica, no atadas a un tipo de contrato específico
    // (mismo campo NULL que ya usa el sistema para F023/Bitácoras universales)
    @Query(value = "SELECT p.* FROM plantilla_formato p " +
                   "WHERE p.estado = 1 " +
                   "AND p.id_seccion_formato IS NULL " +
                   "AND (LOWER(p.ruta_archivo_plantilla) LIKE '%.doc' OR LOWER(p.ruta_archivo_plantilla) LIKE '%.docx' " +
                   "OR LOWER(p.ruta_archivo_plantilla) LIKE '%.xls' OR LOWER(p.ruta_archivo_plantilla) LIKE '%.xlsx' " +
                   "OR LOWER(p.ruta_archivo_plantilla) LIKE '%.pdf')",
           nativeQuery = true)
    List<PlantillaFormato> findWordExcelSinSeccion();
}