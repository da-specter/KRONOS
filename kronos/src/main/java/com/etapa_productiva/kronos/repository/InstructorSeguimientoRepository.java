package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.InstructorSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InstructorSeguimientoRepository extends JpaRepository<InstructorSeguimiento, Long> {
    // Ubica el perfil de Instructor de Seguimiento a partir del Usuario logueado
    Optional<InstructorSeguimiento> findByUsuarioIdUsuario(Long idUsuario);
}