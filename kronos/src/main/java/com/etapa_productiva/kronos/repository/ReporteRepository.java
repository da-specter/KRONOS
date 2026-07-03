package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    List<Reporte> findByUsuarioIdUsuarioOrderByFechaGeneracionDesc(Long idUsuario);
}