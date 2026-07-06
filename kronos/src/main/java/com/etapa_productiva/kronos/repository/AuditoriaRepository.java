package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.AccionAuditoria;
import com.etapa_productiva.kronos.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    // 🔍 Módulo de Auditoría del Administrador: búsqueda con filtros opcionales
    // (acción, texto en la descripción y rango de fechas). Los filtros NULL se ignoran.
    @Query("SELECT a FROM Auditoria a WHERE " +
           "(:accion IS NULL OR a.accion = :accion) AND " +
           "(:texto IS NULL OR LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))) AND " +
           "(:desde IS NULL OR a.fecha >= :desde) AND " +
           "(:hasta IS NULL OR a.fecha <= :hasta) " +
           "ORDER BY a.fecha DESC")
    List<Auditoria> buscarConFiltros(@Param("accion") AccionAuditoria accion,
                                     @Param("texto") String texto,
                                     @Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta);

    List<Auditoria> findTop200ByOrderByFechaDesc();
}
