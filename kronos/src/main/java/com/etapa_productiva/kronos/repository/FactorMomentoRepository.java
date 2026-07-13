package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.FactorMomento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FactorMomentoRepository extends JpaRepository<FactorMomento, Long> {
    List<FactorMomento> findByEvaluacionMomentoIdEvaluacionMomentoOrderByIdFactorAsc(Long idEvaluacionMomento);
}
