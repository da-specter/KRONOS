package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.NivelFormacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NivelFormacionRepository extends JpaRepository<NivelFormacion, Long> {
    // Importación: el nivel debe existir ya en el catálogo (evita duplicados por variantes de tildes/mayúsculas)
    Optional<NivelFormacion> findByNombreNivelIgnoreCase(String nombreNivel);
}