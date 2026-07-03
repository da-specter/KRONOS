package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VisitaSeguimientoRepository extends JpaRepository<VisitaSeguimiento, Long> {
    List<VisitaSeguimiento> findByEtapaProductivaIdEtapaOrderByFechaVisitaDesc(Long idEtapa);
}