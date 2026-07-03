package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Ficha;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaRepository extends JpaRepository<Ficha, Long> {
    // ¡Listo! Ya tienes buscar, guardar, borrar y editar fichas de forma automática.
}