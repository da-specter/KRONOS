package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.DocumentoRequisito;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentoRequisitoRepository extends JpaRepository<DocumentoRequisito, Long> {

    // Buscar todos los documentos que subió un aprendiz para su etapa práctica
    List<DocumentoRequisito> findByEtapaProductivaIdEtapa(Long idEtapa);

    // Bandeja completa del Gestor de Etapa: todos los documentos radicados, más recientes primero
    List<DocumentoRequisito> findAllByOrderByFechaSubidaDesc();

    // Bandeja del Gestor de Etapa: documentos que aún esperan validación
    List<DocumentoRequisito> findByEstadoValidacion(EstadoValidacion estado);

    // Contar cuántos documentos de esa etapa ya fueron APROBADOS por el coordinador
    long countByEtapaProductivaIdEtapaAndEstadoValidacion(Long idEtapa, EstadoValidacion estado);

    // Verificar si un aprendiz ya subió una plantilla específica (Evita duplicados en el Service)
    boolean existsByEtapaProductivaIdEtapaAndPlantillaFormatoIdPlantilla(Long idEtapa, Long idPlantilla);
}