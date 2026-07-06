package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.AreasFormacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreasFormacionRepository extends JpaRepository<AreasFormacion, Long> {

    // Importación de áreas: evita duplicar una coordinación que ya existe en el catálogo
    Optional<AreasFormacion> findByNombreAreaFormacionIgnoreCase(String nombreAreaFormacion);

    List<AreasFormacion> findAllByOrderByNombreAreaFormacionAsc();
}
