package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.ProgramasFormacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgramasFormacionRepository extends JpaRepository<ProgramasFormacion, Long> {

    // Importación: reutiliza el programa si ya existe (por nombre, sin distinguir mayúsculas)
    Optional<ProgramasFormacion> findFirstByNombreProgramaIgnoreCase(String nombrePrograma);
}