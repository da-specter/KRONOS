package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.DocumentoSolicitud;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoSolicitudRepository extends JpaRepository<DocumentoSolicitud, Long> {

    // Documentos ya resubidos para una solicitud (para saber cuáles plantillas ya se diligenciaron)
    List<DocumentoSolicitud> findBySolicitudIdSolicitud(Long idSolicitud);

    // Verifica si una plantilla puntual ya fue resubida para evitar duplicados
    Optional<DocumentoSolicitud> findBySolicitudIdSolicitudAndPlantillaFormatoIdPlantilla(Long idSolicitud, Long idPlantilla);

    // Bandeja del Gestor de Etapa: plantillas firmadas resubidas que aún esperan validación
    List<DocumentoSolicitud> findByEstadoValidacion(EstadoValidacion estado);

    // Bandeja completa del Gestor de Etapa: todas las plantillas firmadas resubidas, más recientes primero
    List<DocumentoSolicitud> findAllByOrderByFechaSubidaDesc();
}
