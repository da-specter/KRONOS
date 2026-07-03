package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Notificacion; // Cambiado a singular
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    // Alimenta el componente de la campana de alertas en el frontend del usuario logueado
    List<Notificacion> findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(Long idUsuario);

    // Historial completo: todas las notificaciones del usuario (leídas y no leídas), más recientes primero
    List<Notificacion> findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);
}