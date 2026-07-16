package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.Notificacion;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.NotificacionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 🔔 Punto único de creación de notificaciones de KRONOS. Toda alerta del sistema pasa
 * por aquí y queda guardada en la entidad NOTIFICACION como historial permanente: marcarla
 * como leída solo cambia su estado (LEIDO = 1), nunca la borra.
 */
@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    /** Crea y persiste una notificación para el usuario destino, sin destino de clic puntual. */
    public Notificacion crear(Usuario usuarioDestino, String mensaje) {
        return crear(usuarioDestino, mensaje, null);
    }

    /**
     * Crea y persiste una notificación para el usuario destino, con la ruta del módulo al
     * que debe llevar un clic sobre ella (ej. "/instructor/seguimiento/bitacoras").
     */
    public Notificacion crear(Usuario usuarioDestino, String mensaje, String urlDestino) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioDestino(usuarioDestino);
        notificacion.setMensaje(mensaje);
        notificacion.setUrlDestino(urlDestino);
        // fechaCreacion y leido=false los fija @PrePersist
        return notificacionRepository.save(notificacion);
    }

    /** No leídas del usuario (alimenta la campana). */
    public List<Notificacion> noLeidas(Long idUsuario) {
        return notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(idUsuario);
    }

    /** Historial completo del usuario (leídas y no leídas), más recientes primero. */
    public List<Notificacion> historial(Long idUsuario) {
        return notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(idUsuario);
    }
}
