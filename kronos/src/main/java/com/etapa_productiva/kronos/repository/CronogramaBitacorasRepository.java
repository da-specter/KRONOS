package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EstadoBitacora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CronogramaBitacorasRepository extends JpaRepository<CronogramaBitacoras, Long> {
    // Muestra las 12 fechas quincenales límites de entrega asignadas al aprendiz
    List<CronogramaBitacoras> findByEtapaProductivaIdEtapaOrderByNumeroBitacoraAsc(Long idEtapa);

    // ⏰ Job diario de alertas: cupos que siguen PENDIENTE con FECHA_LIMITE ya vencida y que
    // todavía no recibieron su alerta de atraso (uno de los dos destinatarios independiente del otro)
    List<CronogramaBitacoras> findByEstadoAndFechaLimiteBeforeAndAlertaInstructorEnviadaFalse(
            EstadoBitacora estado, LocalDate fecha);

    List<CronogramaBitacoras> findByEstadoAndFechaLimiteBeforeAndAlertaAprendizEnviadaFalse(
            EstadoBitacora estado, LocalDate fecha);

    // ⏰ Cupos que vencen HOY (todavía no están atrasados) y siguen PENDIENTE: recordatorio al
    // aprendiz de que hoy es el último día para subir esta bitácora.
    List<CronogramaBitacoras> findByEstadoAndFechaLimiteAndAlertaVenceHoyEnviadaFalse(
            EstadoBitacora estado, LocalDate fecha);
}