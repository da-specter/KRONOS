package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.HistorialNovedad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialNovedadRepository extends JpaRepository<HistorialNovedad, Long> {

    // Recupera todo el hilo de discusión de una novedad ordenado desde el comentario más viejo al más reciente
    List<HistorialNovedad> findByNovedadIdNovedadOrderByFechaAccionAsc(Long idNovedad);
}