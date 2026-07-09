package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EstadoBitacora;
import com.etapa_productiva.kronos.entity.JobEjecucion;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.CronogramaBitacorasRepository;
import com.etapa_productiva.kronos.repository.JobEjecucionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ⏰ Job diario de alertas de bitácoras atrasadas: recorre los cupos del cronograma que ya
 * vencieron (FECHA_LIMITE en el pasado) y siguen PENDIENTE —el aprendiz no radicó su Bitácora—
 * y notifica tanto al Instructor de Seguimiento asignado como al propio aprendiz (además por
 * correo si el envío está habilitado). Corre una vez al día, después del job de visitas, para
 * evitar duplicar la misma alerta.
 *
 * Los días de gracia después de la fecha límite (0 por defecto: se alerta apenas vence) se leen
 * "en caliente" de Configuración Global, y cada corrida queda registrada en JOB_EJECUCION para
 * el módulo "Monitoreo de Jobs" del Administrador.
 */
@Service
public class BitacoraAlertaService {

    public static final String NOMBRE_JOB = "Alertas de bitácoras atrasadas (1:15 a.m.)";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private CronogramaBitacorasRepository cronogramaBitacorasRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JobEjecucionRepository jobEjecucionRepository;

    @Autowired
    private ConfiguracionGlobalService configuracionGlobalService;

    @Scheduled(cron = "0 15 1 * * *") // Todos los días a la 1:15 a.m., después del job de visitas
    @Transactional
    public void revisarBitacorasAtrasadas() {
        LocalDateTime inicio = LocalDateTime.now();
        int diasGracia = configuracionGlobalService.getEntero(ConfiguracionGlobalService.DIAS_ATRASO_BITACORA, 0);
        LocalDate fechaLimiteCorte = LocalDate.now().minusDays(diasGracia);

        int alertasInstructores = 0, alertasAprendices = 0, correosEnviados = 0;
        try {
            alertasInstructores = alertarInstructores(fechaLimiteCorte);

            int[] resultadoAprendices = alertarAprendices(fechaLimiteCorte);
            alertasAprendices = resultadoAprendices[0];
            correosEnviados = resultadoAprendices[1];

            jobEjecucionRepository.save(JobEjecucion.builder()
                    .nombreJob(NOMBRE_JOB)
                    .fechaInicio(inicio)
                    .fechaFin(LocalDateTime.now())
                    .exito(true)
                    .alertasInstructores(alertasInstructores)
                    .alertasAprendices(alertasAprendices)
                    .correosEnviados(correosEnviados)
                    .detalle("Bitácoras atrasadas (con " + diasGracia + " día(s) de gracia): "
                            + alertasInstructores + " alerta(s) a instructores, "
                            + alertasAprendices + " alerta(s) a aprendices.")
                    .build());
        } catch (Exception e) {
            jobEjecucionRepository.save(JobEjecucion.builder()
                    .nombreJob(NOMBRE_JOB)
                    .fechaInicio(inicio)
                    .fechaFin(LocalDateTime.now())
                    .exito(false)
                    .alertasInstructores(alertasInstructores)
                    .alertasAprendices(alertasAprendices)
                    .correosEnviados(correosEnviados)
                    .detalle("Error: " + e.getMessage())
                    .build());
            throw e;
        }
    }

    private int alertarInstructores(LocalDate fechaLimiteCorte) {
        int alertas = 0;
        for (CronogramaBitacoras cupo : cronogramaBitacorasRepository
                .findByEstadoAndFechaLimiteBeforeAndAlertaInstructorEnviadaFalse(EstadoBitacora.PENDIENTE, fechaLimiteCorte)) {

            AsignacionInstructorEtapa asignacion = asignacionInstructorEtapaRepository
                    .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(cupo.getEtapaProductiva().getIdEtapa())
                    .orElse(null);
            if (asignacion == null) {
                continue; // Sin instructor asignado todavía: se reintenta en la próxima corrida
            }

            Usuario aprendiz = cupo.getEtapaProductiva().getAprendizFicha().getUsuario();
            notificacionService.crear(asignacion.getInstructor().getUsuario(),
                    "⏰ " + aprendiz.getNombre() + " " + aprendiz.getApellido() + " está atrasado con la Bitácora "
                            + cupo.getNumeroBitacora() + " (venció el " + cupo.getFechaLimite().format(FORMATO_FECHA) + ").");

            cupo.setAlertaInstructorEnviada(true);
            cronogramaBitacorasRepository.save(cupo);
            alertas++;
        }
        return alertas;
    }

    private int[] alertarAprendices(LocalDate fechaLimiteCorte) {
        int alertas = 0, correos = 0;
        for (CronogramaBitacoras cupo : cronogramaBitacorasRepository
                .findByEstadoAndFechaLimiteBeforeAndAlertaAprendizEnviadaFalse(EstadoBitacora.PENDIENTE, fechaLimiteCorte)) {

            Usuario aprendiz = cupo.getEtapaProductiva().getAprendizFicha().getUsuario();
            String mensaje = "⏰ Estás atrasado con la entrega de tu Bitácora " + cupo.getNumeroBitacora()
                    + ": venció el " + cupo.getFechaLimite().format(FORMATO_FECHA) + ". Súbela cuanto antes.";

            notificacionService.crear(aprendiz, mensaje);
            cupo.setAlertaAprendizEnviada(true);
            cronogramaBitacorasRepository.save(cupo);
            alertas++;
            if (emailService.enviarSiHabilitado(aprendiz.getCorreoElectronico(),
                    "KRONOS - Bitácora atrasada", mensaje)) {
                correos++;
            }
        }
        return new int[]{alertas, correos};
    }
}
