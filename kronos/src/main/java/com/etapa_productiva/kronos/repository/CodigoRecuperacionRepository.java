package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.CodigoRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    // Toma el código vigente más reciente que el usuario generó para validar lo que digitó
    Optional<CodigoRecuperacion> findTopByUsuarioIdUsuarioAndCodigoAndUsadoFalseOrderByIdCodigoDesc(Long idUsuario, String codigo);
}
