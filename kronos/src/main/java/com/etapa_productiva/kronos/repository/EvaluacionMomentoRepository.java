package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.EvaluacionMomento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionMomentoRepository extends JpaRepository<EvaluacionMomento, Long> {
    List<EvaluacionMomento> findByEtapaProductivaIdEtapaOrderByNumeroMomentoAsc(Long idEtapa);

    Optional<EvaluacionMomento> findByEtapaProductivaIdEtapaAndNumeroMomento(Long idEtapa, Integer numeroMomento);

    int countByEtapaProductivaIdEtapa(Long idEtapa);
}
