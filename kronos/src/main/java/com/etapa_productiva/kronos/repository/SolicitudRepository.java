
package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.entity.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends JpaRepository<SolicitudEtapaPractica, Long> {

    /**
     * 👨‍🎓 ÚTIL PARA EL APRENDIZ:
     * Busca la solicitud asociada a la matrícula específica del estudiante.
     * Te sirve para saber si ya inició un proceso o para mostrarle su estado actual en su panel.
     */
    Optional<SolicitudEtapaPractica> findByAprendizFichaIdAprendizFicha(Long idAprendizFicha);

    /**
     * 👨‍💼 ÚTIL PARA EL COORDINADOR:
     * Encuentra todas las solicitudes que estén en un estado específico.
     * Ejemplo: buscar todas las que estén en 'PENDIENTE_REVISION' para armar la bandeja de entrada.
     */
    List<SolicitudEtapaPractica> findByEstado(EstadoSolicitud estado);

    /**
     * 🔍 FILTRO AVANZADO:
     * Trae las solicitudes que coincidan con varios estados al tiempo.
     * Útil si el coordinador quiere ver en una sola lista las 'PENDIENTE_REVISION' y las 'FORMATOS_ENVIADOS'.
     */
    List<SolicitudEtapaPractica> findByEstadoIn(List<EstadoSolicitud> estados);
    
    /**
     * Verifica si un aprendiz ya tiene una solicitud activa para no dejarlo crear otra doble.
     */
    boolean existsByAprendizFichaIdAprendizFichaAndEstadoNot(Long idAprendizFicha, EstadoSolicitud estado);

    /**
     * 🗂️ ÚTIL PARA EL GESTOR DE ETAPA:
     * Solicitudes que ya enviaron sus formatos iniciales pero cuyo panel de plantillas
     * aún no ha sido habilitado (paso adicional, no reemplaza el flujo del Coordinador).
     */
    List<SolicitudEtapaPractica> findByEstadoAndPlantillasHabilitadas(EstadoSolicitud estado, boolean plantillasHabilitadas);

    /**
     * 🎓 ÚTIL PARA COORDINACIÓN ACADÉMICA:
     * Todas las solicitudes, más recientes primero, para su panel de solo lectura.
     */
    List<SolicitudEtapaPractica> findAllByOrderByFechaActualizacionDesc();
}