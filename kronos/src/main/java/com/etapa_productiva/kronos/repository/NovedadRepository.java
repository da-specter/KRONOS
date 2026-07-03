package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Novedad;
import com.etapa_productiva.kronos.entity.EstadoFiltro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NovedadRepository extends JpaRepository<Novedad, Long> {

    // Bandeja de Entrada del Coordinador: Ver trámites pendientes en el Centro de Formación
    List<Novedad> findByDestinatarioAcIdUsuarioAndEstadoFiltroOrderByFechaCreacionDesc(
        Long idCoordinador, 
        EstadoFiltro estado
    );

    // Historial del Aprendiz: Ver el estado de todas las solicitudes que ha radicado
    List<Novedad> findByRemitenteIdUsuarioOrderByFechaCreacionDesc(Long idAprendiz);

    // Filtrar novedades generales por estado (Para los paneles Kanban o contadores de la UI)
    long countByEstadoFiltro(EstadoFiltro estado);

    // 📢 Módulo "Novedades" del Instructor de Seguimiento / Gestor de Etapa: todo lo dirigido a él, sin importar el estado
    List<Novedad> findByDestinatarioAcIdUsuarioOrderByFechaCreacionDesc(Long idUsuarioDestinatario);
}