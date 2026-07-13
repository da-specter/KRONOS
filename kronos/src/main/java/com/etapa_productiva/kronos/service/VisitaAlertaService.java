package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.EstadoEtapa;
import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.JobEjecucion;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.JobEjecucionRepository;
import com.etapa_productiva.kronos.repository.VisitaSeguimientoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ⏰ Job diario de alertas de visitas de seguimiento: recorre las visitas aún PLANEADA y,
 * según cuántos días falten para la fecha agendada, notifica al Instructor de Seguimiento
 * y al Aprendiz (además por correo si el envío está habilitado). También alerta al Instructor
 * de Seguimiento cuando ya pasaron los días configurados (15 por defecto) desde que inició la
 * Etapa Productiva de un aprendiz suyo y todavía no le ha agendado su primera visita. Corre
 * una vez al día para evitar duplicar la misma alerta.
 *
 * Los días de anticipación/espera (3 para el instructor, 2 para el aprendiz, 15 para la
 * primera visita, todos por defecto) se leen "en caliente" de Configuración Global, y cada
 * corrida queda registrada en JOB_EJECUCION para el módulo "Monitoreo de Jobs" del Administrador.
 */
@Service
public class VisitaAlertaService {

    public static final String NOMBRE_JOB = "Alertas de visitas (1:00 a.m.)";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private VisitaSeguimientoRepository visitaSeguimientoRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private JobEjecucionRepository jobEjecucionRepository;

    @Autowired
    private ConfiguracionGlobalService configuracionGlobalService;

    @Value("${whatsapp.plantilla.recordatorio-visita-instructor:recordatorio_visita_instructor}")
    private String plantillaVisitaInstructor;

    @Value("${whatsapp.plantilla.recordatorio-visita-aprendiz:recordatorio_visita_aprendiz}")
    private String plantillaVisitaAprendiz;

    @Scheduled(cron = "0 0 1 * * *") // Todos los días a la 1:00 a.m.
    @Transactional
    public void revisarAlertasVisitas() {
        LocalDateTime inicio = LocalDateTime.now();
        int diasInstructor = configuracionGlobalService.getEntero(ConfiguracionGlobalService.DIAS_ALERTA_INSTRUCTOR, 3);
        int diasAprendiz = configuracionGlobalService.getEntero(ConfiguracionGlobalService.DIAS_ALERTA_APRENDIZ, 2);
        int diasPrimeraVisita = configuracionGlobalService.getEntero(ConfiguracionGlobalService.DIAS_PRIMERA_VISITA, 15);

        int alertasInstructores = 0, alertasAprendices = 0, correosEnviados = 0, alertasPrimeraVisita = 0, whatsappEnviados = 0;
        try {
            LocalDate hoy = LocalDate.now();
            int[] resultadoInstructores = alertarInstructores(hoy, hoy.plusDays(diasInstructor), diasInstructor);
            alertasInstructores = resultadoInstructores[0];
            whatsappEnviados += resultadoInstructores[1];

            int[] resultadoAprendices = alertarAprendices(hoy, hoy.plusDays(diasAprendiz), diasAprendiz);
            alertasAprendices = resultadoAprendices[0];
            correosEnviados = resultadoAprendices[1];
            whatsappEnviados += resultadoAprendices[2];

            alertasPrimeraVisita = alertarPrimeraVisitaPendiente(hoy, diasPrimeraVisita);

            jobEjecucionRepository.save(JobEjecucion.builder()
                    .nombreJob(NOMBRE_JOB)
                    .fechaInicio(inicio)
                    .fechaFin(LocalDateTime.now())
                    .exito(true)
                    .alertasInstructores(alertasInstructores)
                    .alertasAprendices(alertasAprendices)
                    .correosEnviados(correosEnviados)
                    .alertasPrimeraVisita(alertasPrimeraVisita)
                    .registrosProcesados(whatsappEnviados)
                    .detalle("Alertas a " + diasInstructor + " días (instructores) y " + diasAprendiz
                            + " días (aprendices) enviadas correctamente. Alertas de primera visita ("
                            + diasPrimeraVisita + " días desde el inicio): " + alertasPrimeraVisita
                            + ". WhatsApp enviados: " + whatsappEnviados + ".")
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
                    .alertasPrimeraVisita(alertasPrimeraVisita)
                    .registrosProcesados(whatsappEnviados)
                    .detalle("Error: " + e.getMessage())
                    .build());
            throw e;
        }
    }

    // @return [alertas enviadas, mensajes de WhatsApp enviados]
    private int[] alertarInstructores(LocalDate hoy, LocalDate fechaLimite, int dias) {
        int alertas = 0, whatsapps = 0;
        for (VisitaSeguimiento visita : visitaSeguimientoRepository
                .findByEstadoVisitaAndFechaVisitaBetweenAndAlertaInstructorEnviadaFalse(
                        EstadoVisita.PLANEADA, hoy.atStartOfDay(), fechaLimite.atTime(23, 59, 59))) {
            Usuario aprendiz = visita.getEtapaProductiva().getAprendizFicha().getUsuario();
            Usuario instructor = visita.getInstructor();
            String fechaVisita = visita.getFechaVisita().format(FORMATO_FECHA);

            notificacionService.crear(instructor,
                    "⏰ Tienes una visita de seguimiento programada en " + dias + " días ("
                            + fechaVisita + ") con " + aprendiz.getNombre() + " " + aprendiz.getApellido() + ".");
            visita.setAlertaInstructorEnviada(true);
            visitaSeguimientoRepository.save(visita);
            alertas++;

            if (whatsAppService.enviarPlantillaSiHabilitado(instructor.getTelefono(), plantillaVisitaInstructor, "es",
                    List.of(instructor.getNombre(), aprendiz.getNombre() + " " + aprendiz.getApellido(), fechaVisita))) {
                whatsapps++;
            }
        }
        return new int[]{alertas, whatsapps};
    }

    // @return [alertas enviadas, correos enviados, mensajes de WhatsApp enviados]
    private int[] alertarAprendices(LocalDate hoy, LocalDate fechaLimite, int dias) {
        int alertas = 0, correos = 0, whatsapps = 0;
        for (VisitaSeguimiento visita : visitaSeguimientoRepository
                .findByEstadoVisitaAndFechaVisitaBetweenAndAlertaAprendizEnviadaFalse(
                        EstadoVisita.PLANEADA, hoy.atStartOfDay(), fechaLimite.atTime(23, 59, 59))) {
            Usuario aprendiz = visita.getEtapaProductiva().getAprendizFicha().getUsuario();
            String fechaVisita = visita.getFechaVisita().format(FORMATO_FECHA);
            String mensaje = "⏰ Recuerda: tienes una visita de seguimiento programada en " + dias + " días ("
                    + fechaVisita + ").";

            notificacionService.crear(aprendiz, mensaje);
            visita.setAlertaAprendizEnviada(true);
            visitaSeguimientoRepository.save(visita);
            alertas++;
            if (emailService.enviarSiHabilitado(aprendiz.getCorreoElectronico(),
                    "KRONOS - Recordatorio de visita de seguimiento", mensaje)) {
                correos++;
            }
            if (whatsAppService.enviarPlantillaSiHabilitado(aprendiz.getTelefono(), plantillaVisitaAprendiz, "es",
                    List.of(aprendiz.getNombre(), fechaVisita))) {
                whatsapps++;
            }
        }
        return new int[]{alertas, correos, whatsapps};
    }

    /**
     * 📅 Alerta al Instructor de Seguimiento asignado cuando ya pasaron {@code diasPrimeraVisita}
     * días desde FECHA_INICIO de la Etapa Productiva y todavía no le ha agendado ninguna visita.
     * Se envía una sola vez por etapa (bandera ALERTA_PRIMERA_VISITA_ENVIADA); las etapas sin
     * instructor asignado todavía o que ya tienen alguna visita agendada se saltan sin marcar la
     * bandera, para que el job las vuelva a evaluar en corridas futuras.
     */
    private int alertarPrimeraVisitaPendiente(LocalDate hoy, int diasPrimeraVisita) {
        int alertas = 0;
        for (EtapaProductiva etapa : etapaProductivaRepository
                .findByEstadoEtapaAndAlertaPrimeraVisitaEnviadaFalseAndFechaInicioLessThanEqual(
                        EstadoEtapa.EN_PROGRESO, hoy.minusDays(diasPrimeraVisita))) {

            if (visitaSeguimientoRepository.existsByEtapaProductivaIdEtapa(etapa.getIdEtapa())) {
                continue; // Ya tiene al menos una visita agendada: no hace falta alertar
            }

            AsignacionInstructorEtapa asignacion = asignacionInstructorEtapaRepository
                    .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(etapa.getIdEtapa())
                    .orElse(null);
            if (asignacion == null) {
                continue; // Sin instructor asignado todavía: se reintenta en la próxima corrida
            }

            Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
            notificacionService.crear(asignacion.getInstructor().getUsuario(),
                    "⏰ Ya pasaron " + diasPrimeraVisita + " días desde que inició la Etapa Productiva de "
                            + aprendiz.getNombre() + " " + aprendiz.getApellido()
                            + ": agenda su primera visita de seguimiento.");

            etapa.setAlertaPrimeraVisitaEnviada(true);
            etapaProductivaRepository.save(etapa);
            alertas++;
        }
        return alertas;
    }
}
