package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.CodigoRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    // Toma el código vigente más reciente que el usuario generó para validar lo que digitó
    Optional<CodigoRecuperacion> findTopByUsuarioIdUsuarioAndCodigoAndUsadoFalseOrderByIdCodigoDesc(Long idUsuario, String codigo);

    // 🧹 Job de limpieza: borra códigos cuya ventana de 15 minutos venció hace más de N días
    // (usados o no; una vez vencidos son inútiles). Devuelve cuántas filas eliminó.
    long deleteByFechaExpiracionBefore(LocalDateTime corte);
}
