package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.NivelFormacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NivelFormacionRepository extends JpaRepository<NivelFormacion, Long> {
}