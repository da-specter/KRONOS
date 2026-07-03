
package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.InstructorTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InstructorTecnicoRepository extends JpaRepository<InstructorTecnico, Long> {
    // Ubica el perfil de Instructor Técnico a partir del Usuario logueado
    Optional<InstructorTecnico> findByUsuarioIdUsuario(Long idUsuario);
}