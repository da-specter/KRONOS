package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.EvaluacionBitacora; // Cambiado a singular
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EvaluacionBitacoraRepository extends JpaRepository<EvaluacionBitacora, Long> {
    // Última evaluación registrada para una bitácora (por si el aprendiz corrigió y se reevaluó)
    Optional<EvaluacionBitacora> findTopByBitacoraIdBitacoraOrderByFechaEvaluacionDesc(Long idBitacora);
}