package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Ficha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FichaRepository extends JpaRepository<Ficha, Long> {
    // ¡Listo! Ya tienes buscar, guardar, borrar y editar fichas de forma automática.

    // Importación de fichas: ubica una ficha por su número para no duplicarla
    Optional<Ficha> findByNumeroFicha(String numeroFicha);
}