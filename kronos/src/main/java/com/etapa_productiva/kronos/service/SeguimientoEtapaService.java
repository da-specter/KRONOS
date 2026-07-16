package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.Bitacora;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EstadoBitacora;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.EvaluacionBitacora;
import com.etapa_productiva.kronos.entity.ResultadoEvaluacion;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.BitacoraRepository;
import com.etapa_productiva.kronos.repository.CronogramaBitacorasRepository;
import com.etapa_productiva.kronos.repository.EvaluacionBitacoraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 📚 Sección "Subir Bitácoras" del Aprendiz: Formato de Planeación (1 por Etapa Productiva)
 * y Bitácoras de seguimiento (una por cupo del Cronograma).
 */
@Service
public class SeguimientoEtapaService {

    @Autowired
    private CronogramaBitacorasRepository cronogramaBitacorasRepository;

    @Autowired
    private BitacoraRepository bitacoraRepository;

    @Autowired
    private EvaluacionBitacoraRepository evaluacionBitacoraRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    @Transactional
    public Bitacora subirBitacora(EtapaProductiva etapa, Long idCronograma, String asunto, MultipartFile archivo) {
        CronogramaBitacoras cronograma = cronogramaBitacorasRepository.findById(idCronograma)
                .orElseThrow(() -> new RuntimeException("Cupo de cronograma no encontrado con ID: " + idCronograma));

        if (!cronograma.getEtapaProductiva().getIdEtapa().equals(etapa.getIdEtapa())) {
            throw new IllegalArgumentException("Ese cupo de bitácora no pertenece a tu Etapa Productiva.");
        }

        // ⏳ Candado de calendario: la bitácora solo se puede radicar desde su fecha de apertura.
        // Después de la fecha límite sigue permitida (entrega extemporánea), antes de tiempo no.
        // 🚧 DESHABILITADO TEMPORALMENTE (a pedido del usuario) para poder probar el flujo de
        // momentos sin esperar las fechas reales del cronograma. Reactivar el bloque de abajo
        // cuando termine la etapa de pruebas.
        // if (java.time.LocalDate.now().isBefore(cronograma.getFechaApertura())) {
        //     throw new IllegalStateException("La Bitácora N°" + cronograma.getNumeroBitacora()
        //             + " aún no está habilitada: se abre el "
        //             + cronograma.getFechaApertura().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
        // }

        // Última bitácora radicada para ese cupo (si existe): solo se permite volver a radicar
        // si el Instructor Técnico pidió corrección o la reprobó; si está en revisión o aprobada, no.
        Bitacora anterior = bitacoraRepository.findByEtapaProductivaIdEtapaOrderByFechaEntregaDesc(etapa.getIdEtapa())
                .stream()
                .filter(b -> b.getCronogramaBitacora().getIdCronograma().equals(idCronograma))
                .findFirst()
                .orElse(null);
        if (anterior != null) {
            EvaluacionBitacora ultima = evaluacionBitacoraRepository
                    .findTopByBitacoraIdBitacoraOrderByFechaEvaluacionDesc(anterior.getIdBitacora())
                    .orElse(null);
            boolean puedeResubir = ultima != null
                    && (ultima.getResultado() == ResultadoEvaluacion.CORREGIR || ultima.getResultado() == ResultadoEvaluacion.REPROBADO);
            if (!puedeResubir) {
                throw new IllegalStateException(ultima == null
                        ? "Ya radicaste la Bitácora N°" + cronograma.getNumeroBitacora() + "; está en revisión por tu Instructor Técnico."
                        : "Ya radicaste la Bitácora N°" + cronograma.getNumeroBitacora() + " y fue aprobada.");
            }
        }

        String rutaGuardada = guardarArchivo(archivo, "bitacoras", etapa.getIdEtapa());

        Bitacora bitacora = Bitacora.builder()
                .cronogramaBitacora(cronograma)
                .asunto(asunto)
                .rutaArchivo(rutaGuardada)
                .build();

        Bitacora guardada = bitacoraRepository.save(bitacora);

        cronograma.setEstado(EstadoBitacora.ENTREGADA);
        cronogramaBitacorasRepository.save(cronograma);

        notificarInstructorSeguimiento(etapa, "📓 %s subió la Bitácora N°" + cronograma.getNumeroBitacora()
                + " (\"" + asunto + "\"). Ya puedes evaluarla en KRONOS.");

        return guardada;
    }

    // 🔔 Notifica al Instructor de Seguimiento vigente de la etapa (si ya tiene uno asignado).
    // El %s del mensaje se reemplaza por el nombre completo del aprendiz.
    private void notificarInstructorSeguimiento(EtapaProductiva etapa, String plantillaMensaje) {
        AsignacionInstructorEtapa asignacion = asignacionInstructorEtapaRepository
                .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(etapa.getIdEtapa())
                .orElse(null);
        if (asignacion == null) {
            return; // Sin instructor asignado todavía: no hay a quién notificar
        }
        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
        notificacionService.crear(asignacion.getInstructor().getUsuario(),
                plantillaMensaje.replace("%s", aprendiz.getNombre() + " " + aprendiz.getApellido()),
                "/instructor/seguimiento/bitacoras");
    }

    private String guardarArchivo(MultipartFile archivo, String subcarpeta, Long idEtapa) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar un archivo para subir.");
        }

        try {
            Path directorio = Paths.get(uploadRootDir, subcarpeta, "etapa_" + idEtapa);
            Files.createDirectories(directorio);
            String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
            int puntoIdx = nombreOriginal.lastIndexOf('.');
            String extension = puntoIdx >= 0 ? nombreOriginal.substring(puntoIdx).toLowerCase() : "";
            String nombreArchivo = java.util.UUID.randomUUID() + extension;
            Path destino = directorio.resolve(nombreArchivo);
            archivo.transferTo(destino);
            return "/" + destino.toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo en el servidor: " + e.getMessage(), e);
        }
    }
}
