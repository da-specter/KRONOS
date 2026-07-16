package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.JobEjecucion;
import com.etapa_productiva.kronos.repository.JobEjecucionRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 🧹 Job de limpieza de NOTIFICACIONES: cada acción del flujo (bitácora evaluada, visita
 * agendada, solicitud radicada, etc.) crea una fila y nunca se borra sola, así que la tabla
 * crece sin límite con el tiempo. Este job borra las que ya llevan más de N días desde su
 * creación (leídas o no; 30 por defecto, ajustable "en caliente" en Configuración Global),
 * corriendo cada 2 días para no competir con los jobs diarios de alertas/visitas/bitácoras.
 * Cada corrida queda registrada en JOB_EJECUCION para el módulo "Monitoreo de Jobs" del Administrador.
 */
@Service
public class NotificacionLimpiezaService {

    public static final String NOMBRE_JOB = "Limpieza de notificaciones (cada 2 días, 1:45 a.m.)";

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private JobEjecucionRepository jobEjecucionRepository;

    @Autowired
    private ConfiguracionGlobalService configuracionGlobalService;

    // 1:45 a.m. de cada día par del mes (día 2, 4, 6...): después de visitas/bitácoras/códigos
    // de recuperación, y con la periodicidad "cada 2 días" que no ofrece un solo campo de cron.
    @Scheduled(cron = "0 45 1 */2 * *")
    @Transactional
    public void limpiarNotificacionesAntiguas() {
        LocalDateTime inicio = LocalDateTime.now();
        int diasRetencion = configuracionGlobalService.getEntero(
                ConfiguracionGlobalService.DIAS_RETENCION_NOTIFICACIONES, 30);
        long eliminadas = 0;
        try {
            LocalDateTime corte = LocalDateTime.now().minusDays(diasRetencion);
            eliminadas = notificacionRepository.deleteByFechaCreacionBefore(corte);

            jobEjecucionRepository.save(JobEjecucion.builder()
                    .nombreJob(NOMBRE_JOB)
                    .fechaInicio(inicio)
                    .fechaFin(LocalDateTime.now())
                    .exito(true)
                    .alertasInstructores(0)
                    .alertasAprendices(0)
                    .correosEnviados(0)
                    .registrosProcesados((int) eliminadas)
                    .detalle(eliminadas + " notificación(es) de más de " + diasRetencion + " día(s) eliminada(s).")
                    .build());
        } catch (Exception e) {
            jobEjecucionRepository.save(JobEjecucion.builder()
                    .nombreJob(NOMBRE_JOB)
                    .fechaInicio(inicio)
                    .fechaFin(LocalDateTime.now())
                    .exito(false)
                    .alertasInstructores(0)
                    .alertasAprendices(0)
                    .correosEnviados(0)
                    .registrosProcesados((int) eliminadas)
                    .detalle("Error: " + e.getMessage())
                    .build());
            throw e;
        }
    }
}
