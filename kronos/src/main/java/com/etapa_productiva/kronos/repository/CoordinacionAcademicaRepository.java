package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.CoordinacionAcademica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoordinacionAcademicaRepository extends JpaRepository<CoordinacionAcademica, Long> {
}