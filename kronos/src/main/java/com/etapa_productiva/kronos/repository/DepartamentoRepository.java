package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {
    // Evita duplicar el mismo departamento cuando el Gestor lo escribe libremente en el formulario
    Optional<Departamento> findByNombreDepartamentoIgnoreCase(String nombreDepartamento);
}