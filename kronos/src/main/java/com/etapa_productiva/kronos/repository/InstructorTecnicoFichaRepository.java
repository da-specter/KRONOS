package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.InstructorTecnicoFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstructorTecnicoFichaRepository extends JpaRepository<InstructorTecnicoFicha, Long> {
    List<InstructorTecnicoFicha> findByFichaIdFichaAndEstadoTrue(Long idFicha);

    // Panel del Instructor Técnico: fichas activas que tiene asignadas para revisión
    List<InstructorTecnicoFicha> findByInstructorTecnicoIdInstructorTecnicoAndEstadoTrue(Long idInstructorTecnico);
}