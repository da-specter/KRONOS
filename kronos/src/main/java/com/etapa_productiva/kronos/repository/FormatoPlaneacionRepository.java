package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.FormatoPlaneacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FormatoPlaneacionRepository extends JpaRepository<FormatoPlaneacion, Long> {
    Optional<FormatoPlaneacion> findByEtapaProductivaIdEtapa(Long idEtapa);
}