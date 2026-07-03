package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.EvaluacionPlaneacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluacionPlaneacionRepository extends JpaRepository<EvaluacionPlaneacion, Long> {
}