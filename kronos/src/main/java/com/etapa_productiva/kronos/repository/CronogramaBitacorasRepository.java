package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CronogramaBitacorasRepository extends JpaRepository<CronogramaBitacoras, Long> {
    // Muestra las 12 fechas quincenales límites de entrega asignadas al aprendiz
    List<CronogramaBitacoras> findByEtapaProductivaIdEtapaOrderByNumeroBitacoraAsc(Long idEtapa);
}