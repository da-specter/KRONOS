package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.InstructorSeguimientoFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstructorSeguimientoFichaRepository extends JpaRepository<InstructorSeguimientoFicha, Long> {
    List<InstructorSeguimientoFicha> findByFichaIdFichaAndEstadoTrue(Long idFicha);

    // Panel del Instructor de Seguimiento: fichas activas que tiene asignadas
    List<InstructorSeguimientoFicha> findByInstructorSeguimientoIdInstructorSeguimientoAndEstadoTrue(Long idInstructorSeguimiento);
}