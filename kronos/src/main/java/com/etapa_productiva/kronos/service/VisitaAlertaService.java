package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.JobEjecucion;
import com.etapa_productiva.kronos.entity.TipoVisita;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import com.etapa_productiva.kronos.repository.JobEjecucionRepository;
import com.etapa_productiva.kronos.repository.VisitaSeguimientoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ⏰ Job diario de alertas de visitas de seguimiento: recorre las visitas aún PLANEADA y,
 * según cuántos días falten para la fecha agendada, notifica al Instructor de Seguimiento
 * y al Aprendiz (además por correo si el envío está habilitado). Corre una vez al día para
 * evitar duplicar la misma alerta.
 *
 * Los días de anticipación (3 para el instructor, 2 para el aprendiz por defecto) se leen
 * "en caliente" de Configuración Global, y cada corrida queda registrada en JOB_EJECUCION
 * para el módulo "Monitoreo de Jobs" del Administrador.
 */
@Service
public class VisitaAlertaService {

    public static final String NOMBRE_JOB = "Alertas de visitas (1:00 a.m.)";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private VisitaSeguimientoRepository visitaSeguimientoRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JobEjecucionRepository jobEjecucionRepository;

    @Autowired
    private ConfiguracionGlobalService configuracionGlobalService;

    @Scheduled(cron = "0 0 1 * * *") // Todos los días a la 1:00 a.m.
    @Transactional
    public void revisarAlertasVisitas() {
        LocalDateTime inicio = LocalDateTime.now();
        int diasInstructor = configuracionGlobalService.getEntero(ConfiguracionGlobalService.DIAS_ALERTA_INSTRUCTOR, 3);
        int diasAprendiz = configuracionGlobalService.getEntero(ConfiguracionGlobalService.DIAS_ALERTA_APRENDIZ, 2);

        int alertasInstructores = 0, alertasAprendices = 0, correosEnviados = 0;
        try {
            LocalDate hoy = LocalDate.now();
            alertasInstructores = alertarInstructores(hoy.plusDays(diasInstructor), diasInstructor);

            int[] resultadoAprendices = alertarAprendices(hoy.plusDays(diasAprendiz), diasAprendiz);
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
                    .detalle("Alertas a " + diasInstructor + " días (instructores) y " + diasAprendiz
                            + " días (aprendices) enviadas correctamente.")
                    .build());
        } catch (Exception e) {
            // Registra la corrida fallida para que el Administrador la vea en Monitoreo de Jobs
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

    private int alertarInstructores(LocalDate fechaObjetivo, int dias) {
        int alertas = 0;
        for (VisitaSeguimiento visita : visitaSeguimientoRepository.findByEstadoVisitaAndFechaVisita(EstadoVisita.PLANEADA, fechaObjetivo)) {
            Usuario aprendiz = visita.getEtapaProductiva().getAprendizFicha().getUsuario();
            notificacionService.crear(visita.getInstructor(),
                    "⏰ Tienes una visita " + etiqueta(visita.getTipoVisita()) + " programada en " + dias + " días ("
                            + visita.getFechaVisita().format(FORMATO_FECHA) + ") con "
                            + aprendiz.getNombre() + " " + aprendiz.getApellido() + ".");
            alertas++;
        }
        return alertas;
    }

    private int[] alertarAprendices(LocalDate fechaObjetivo, int dias) {
        int alertas = 0, correos = 0;
        for (VisitaSeguimiento visita : visitaSeguimientoRepository.findByEstadoVisitaAndFechaVisita(EstadoVisita.PLANEADA, fechaObjetivo)) {
            Usuario aprendiz = visita.getEtapaProductiva().getAprendizFicha().getUsuario();
            String mensaje = "⏰ Recuerda: tienes una visita de seguimiento " + etiqueta(visita.getTipoVisita())
                    + " programada en " + dias + " días (" + visita.getFechaVisita().format(FORMATO_FECHA) + ").";

            notificacionService.crear(aprendiz, mensaje);
            alertas++;
            if (emailService.enviarSiHabilitado(aprendiz.getCorreoElectronico(),
                    "KRONOS - Recordatorio de visita de seguimiento", mensaje)) {
                correos++;
            }
        }
        return new int[]{alertas, correos};
    }

    private String etiqueta(TipoVisita tipo) {
        return switch (tipo) {
            case CONCERTACION -> "inicial";
            case SEGUIMIENTO -> "parcial";
            case EVALUACION_FINAL -> "final";
        };
    }
}
