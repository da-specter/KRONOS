package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.AprendizFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AprendizFichaRepository extends JpaRepository<AprendizFicha, Long> {
    // Devuelve los aprendices vinculados a una ficha técnica
    List<AprendizFicha> findByFichaIdFicha(Long idFicha);

    // Ubica la matrícula del aprendiz a partir del Usuario logueado
    Optional<AprendizFicha> findByUsuarioIdUsuario(Long idUsuario);

    // Importación: evita matricular dos veces al mismo aprendiz en la misma ficha
    boolean existsByUsuarioIdUsuarioAndFichaIdFicha(Long idUsuario, Long idFicha);
}