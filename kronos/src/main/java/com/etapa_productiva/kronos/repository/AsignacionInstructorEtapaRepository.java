package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionInstructorEtapaRepository extends JpaRepository<AsignacionInstructorEtapa, Long> {

    // La asignación vigente de una Etapa Productiva (si la hay)
    Optional<AsignacionInstructorEtapa> findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(Long idEtapa);

    // Historial completo de asignaciones de una Etapa Productiva, más reciente primero
    List<AsignacionInstructorEtapa> findByEtapaProductivaIdEtapaOrderByFechaAsignacionDesc(Long idEtapa);
}
