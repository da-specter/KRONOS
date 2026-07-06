package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.JobEjecucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobEjecucionRepository extends JpaRepository<JobEjecucion, Long> {

    // Historial del Monitoreo de Jobs: las últimas 100 corridas, la más reciente primero
    List<JobEjecucion> findTop100ByOrderByFechaInicioDesc();
}
