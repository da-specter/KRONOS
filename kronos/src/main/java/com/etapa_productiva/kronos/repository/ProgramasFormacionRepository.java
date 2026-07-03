package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.ProgramasFormacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramasFormacionRepository extends JpaRepository<ProgramasFormacion, Long> {

}