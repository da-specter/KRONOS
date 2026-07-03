package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.entity.Reporte;
import com.etapa_productiva.kronos.entity.TipoReporte;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.ReporteRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 📑 Historial de reportes de KRONOS: cada exportación (Excel o PDF) que hace un usuario
 * se guarda físicamente en el File Server y se registra en la entidad REPORTE, detectando
 * automáticamente el tipo (CONTRATOS_EXCEL, FICHAS_EXCEL, APRENDIZ_EXCEL o EJECUTIVO).
 */
@Service
public class ReporteService {

    private static final DateTimeFormatter SELLO_TIEMPO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    /**
     * Regla de detección del tipo de reporte: un PDF siempre es un consolidado EJECUTIVO;
     * un Excel toma el tipo según el módulo que lo originó.
     *
     * @param origen módulo que exporta: "APRENDICES" (contratos), "FICHAS" o "APRENDIZ_FICHA"
     */
    public TipoReporte detectarTipo(String origen, FormatoReporte formato) {
        if (formato == FormatoReporte.PDF) {
            return TipoReporte.EJECUTIVO;
        }
        return switch (origen) {
            case "APRENDICES" -> TipoReporte.CONTRATOS_EXCEL;
            case "FICHAS" -> TipoReporte.FICHAS_EXCEL;
            case "APRENDIZ_FICHA", "INSTRUCTOR_APRENDICES" -> TipoReporte.APRENDIZ_EXCEL;
            default -> TipoReporte.EJECUTIVO;
        };
    }

    /**
     * Guarda el archivo generado y registra el Reporte para el usuario indicado.
     * No interrumpe la descarga si algo falla al persistir el historial (best-effort).
     */
    public void registrar(Long idUsuario, String origen, FormatoReporte formato, byte[] contenido) {
        TipoReporte tipo = detectarTipo(origen, formato);
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            if (usuario == null) {
                return;
            }

            String extension = formato == FormatoReporte.PDF ? ".pdf" : ".xlsx";
            Path directorio = Paths.get(uploadRootDir, "reportes", "usuario_" + idUsuario);
            Files.createDirectories(directorio);
            String nombreArchivo = tipo.name().toLowerCase() + "_" + LocalDateTime.now().format(SELLO_TIEMPO) + extension;
            Path destino = directorio.resolve(nombreArchivo);
            Files.write(destino, contenido);

            Reporte reporte = Reporte.builder()
                    .usuario(usuario)
                    .tipoReporte(tipo)
                    .formato(formato)
                    .fechaGeneracion(LocalDateTime.now())
                    .rutaArchivo("/" + destino.toString().replace('\\', '/'))
                    .build();
            reporteRepository.save(reporte);
        } catch (IOException e) {
            // El historial es complementario: si el disco falla, la descarga igual se entrega
            System.out.println("⚠️ [REPORTE] No se pudo registrar el historial del reporte: " + e.getMessage());
        }
    }
}
