package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    // Importación: asigna el rol APRENDIZ a los usuarios recién creados
    Optional<Rol> findByNombreRol(String nombreRol);
}