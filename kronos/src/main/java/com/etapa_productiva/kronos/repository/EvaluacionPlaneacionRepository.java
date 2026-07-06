package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.EvaluacionPlaneacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EvaluacionPlaneacionRepository extends JpaRepository<EvaluacionPlaneacion, Long> {
    // Última evaluación registrada para un Formato de Planeación (por si el aprendiz corrigió y se reevaluó)
    Optional<EvaluacionPlaneacion> findTopByFormatoPlaneacionIdFormatoPlaneacionOrderByFechaEvaluacionDesc(Long idFormatoPlaneacion);
}