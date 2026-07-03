package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByEstadoTrueOrderByFechaGeneracionDesc();
}