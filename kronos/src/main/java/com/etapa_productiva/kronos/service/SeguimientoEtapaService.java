package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.Bitacora;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EstadoBitacora;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.EvaluacionBitacora;
import com.etapa_productiva.kronos.entity.EvaluacionPlaneacion;
import com.etapa_productiva.kronos.entity.FormatoPlaneacion;
import com.etapa_productiva.kronos.entity.ResultadoEvaluacion;
import com.etapa_productiva.kronos.repository.BitacoraRepository;
import com.etapa_productiva.kronos.repository.CronogramaBitacorasRepository;
import com.etapa_productiva.kronos.repository.EvaluacionBitacoraRepository;
import com.etapa_productiva.kronos.repository.EvaluacionPlaneacionRepository;
import com.etapa_productiva.kronos.repository.FormatoPlaneacionRepository;
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
    private FormatoPlaneacionRepository formatoPlaneacionRepository;

    @Autowired
    private CronogramaBitacorasRepository cronogramaBitacorasRepository;

    @Autowired
    private BitacoraRepository bitacoraRepository;

    @Autowired
    private EvaluacionBitacoraRepository evaluacionBitacoraRepository;

    @Autowired
    private EvaluacionPlaneacionRepository evaluacionPlaneacionRepository;

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    @Transactional
    public FormatoPlaneacion subirFormatoPlaneacion(EtapaProductiva etapa, String asunto, MultipartFile archivo) {
        FormatoPlaneacion existente = formatoPlaneacionRepository.findByEtapaProductivaIdEtapa(etapa.getIdEtapa()).orElse(null);

        if (existente != null) {
            // Solo se puede volver a radicar (sobre el mismo registro) si el Instructor Técnico
            // pidió corrección o lo reprobó; si está en revisión o ya aprobado, no se puede resubir.
            EvaluacionPlaneacion ultima = evaluacionPlaneacionRepository
                    .findTopByFormatoPlaneacionIdFormatoPlaneacionOrderByFechaEvaluacionDesc(existente.getIdFormatoPlaneacion())
                    .orElse(null);
            boolean puedeResubir = ultima != null
                    && (ultima.getResultado() == ResultadoEvaluacion.CORREGIR || ultima.getResultado() == ResultadoEvaluacion.REPROBADO);
            if (!puedeResubir) {
                throw new IllegalStateException(ultima == null
                        ? "Ya radicaste el Formato de Planeación; está en revisión por tu Instructor Técnico."
                        : "Ya radicaste el Formato de Planeación y fue aprobado.");
            }

            existente.setAsunto(asunto);
            existente.setRutaArchivo(guardarArchivo(archivo, "formato-planeacion", etapa.getIdEtapa()));
            existente.setFechaEntrega(java.time.LocalDate.now());
            existente.setFechaHoraSubida(java.time.LocalDateTime.now());
            return formatoPlaneacionRepository.save(existente);
        }

        String rutaGuardada = guardarArchivo(archivo, "formato-planeacion", etapa.getIdEtapa());

        FormatoPlaneacion formato = FormatoPlaneacion.builder()
                .etapaProductiva(etapa)
                .asunto(asunto)
                .rutaArchivo(rutaGuardada)
                .build();

        return formatoPlaneacionRepository.save(formato);
    }

    @Transactional
    public Bitacora subirBitacora(EtapaProductiva etapa, Long idCronograma, String asunto, MultipartFile archivo) {
        CronogramaBitacoras cronograma = cronogramaBitacorasRepository.findById(idCronograma)
                .orElseThrow(() -> new RuntimeException("Cupo de cronograma no encontrado con ID: " + idCronograma));

        if (!cronograma.getEtapaProductiva().getIdEtapa().equals(etapa.getIdEtapa())) {
            throw new IllegalArgumentException("Ese cupo de bitácora no pertenece a tu Etapa Productiva.");
        }

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

        return guardada;
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
