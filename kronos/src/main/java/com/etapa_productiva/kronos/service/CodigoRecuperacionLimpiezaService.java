package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.JobEjecucion;
import com.etapa_productiva.kronos.repository.CodigoRecuperacionRepository;
import com.etapa_productiva.kronos.repository.JobEjecucionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 🧹 Job diario de limpieza de CODIGO_RECUPERACION: cada código vive solo 15 minutos
 * (PasswordRecoveryService), pero la tabla nunca los borra por sí sola, así que crece sin
 * límite con el tiempo (usados o simplemente vencidos porque el usuario no los digitó a
 * tiempo). Este job borra los que ya vencieron hace más de N días (7 por defecto, ajustable
 * "en caliente" en Configuración Global), dejando un margen de días antes de borrar por si
 * se necesita revisar el historial reciente. Corre después de los jobs de alertas, y cada
 * corrida queda registrada en JOB_EJECUCION para el módulo "Monitoreo de Jobs" del Administrador.
 */
@Service
public class CodigoRecuperacionLimpiezaService {

    public static final String NOMBRE_JOB = "Limpieza de códigos de recuperación (1:30 a.m.)";

    @Autowired
    private CodigoRecuperacionRepository codigoRecuperacionRepository;

    @Autowired
    private JobEjecucionRepository jobEjecucionRepository;

    @Autowired
    private ConfiguracionGlobalService configuracionGlobalService;

    @Scheduled(cron = "0 30 1 * * *") // Todos los días a la 1:30 a.m., después de visitas y bitácoras
    @Transactional
    public void limpiarCodigosVencidos() {
        LocalDateTime inicio = LocalDateTime.now();
        int diasRetencion = configuracionGlobalService.getEntero(
                ConfiguracionGlobalService.DIAS_RETENCION_CODIGOS_RECUPERACION, 7);
        long eliminados = 0;
        try {
            LocalDateTime corte = LocalDateTime.now().minusDays(diasRetencion);
            eliminados = codigoRecuperacionRepository.deleteByFechaExpiracionBefore(corte);

            jobEjecucionRepository.save(JobEjecucion.builder()
                    .nombreJob(NOMBRE_JOB)
                    .fechaInicio(inicio)
                    .fechaFin(LocalDateTime.now())
                    .exito(true)
                    .alertasInstructores(0)
                    .alertasAprendices(0)
                    .correosEnviados(0)
                    .registrosProcesados((int) eliminados)
                    .detalle(eliminados + " código(s) de recuperación vencido(s) hace más de "
                            + diasRetencion + " día(s) eliminado(s).")
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
                    .registrosProcesados((int) eliminados)
                    .detalle("Error: " + e.getMessage())
                    .build());
            throw e;
        }
    }
}
