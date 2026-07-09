package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.ReporteAprendizDto;
import com.etapa_productiva.kronos.entity.DocumentoSolicitud;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.DocumentoSolicitudRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 📋 "Reporte Aprendiz": evidencia de los documentos requisito ya diligenciados y APROBADOS,
 * buscable por nombre, apellido, documento, tipo de documento, ficha y modalidad de contrato.
 * Usado tanto por el rol Registro como por el Administrador (mismo criterio, misma vista).
 */
@Service
public class ReporteAprendizService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private DocumentoSolicitudRepository documentoSolicitudRepository;

    @Transactional(readOnly = true)
    public List<ReporteAprendizDto> buscar(String nombre, String apellido, String documento,
                                           TipoDocumento tipoDocumento, String ficha, Long idSeccionFormato) {

        String nombreFiltro = normalizar(nombre);
        String apellidoFiltro = normalizar(apellido);
        String documentoFiltro = normalizar(documento);
        String fichaFiltro = normalizar(ficha);

        // Agrupa los documentos APROBADOS por solicitud: cada solicitud es un aprendiz con su
        // ficha y modalidad de contrato puntuales.
        Map<Long, List<DocumentoSolicitud>> porSolicitud = new LinkedHashMap<>();
        for (DocumentoSolicitud doc : documentoSolicitudRepository.findByEstadoValidacion(EstadoValidacion.APROBADO)) {
            SolicitudEtapaPractica solicitud = doc.getSolicitud();
            Usuario aprendiz = solicitud.getAprendizFicha().getUsuario();
            Ficha fichaAprendiz = solicitud.getAprendizFicha().getFicha();

            if (nombreFiltro != null && !normalizar(aprendiz.getNombre()).contains(nombreFiltro)) continue;
            if (apellidoFiltro != null && !normalizar(aprendiz.getApellido()).contains(apellidoFiltro)) continue;
            if (documentoFiltro != null && !normalizar(aprendiz.getDocumento()).contains(documentoFiltro)) continue;
            if (tipoDocumento != null && aprendiz.getTipoDocumento() != tipoDocumento) continue;
            if (fichaFiltro != null && !normalizar(fichaAprendiz.getNumeroFicha()).contains(fichaFiltro)) continue;
            if (idSeccionFormato != null && (solicitud.getSeccionFormato() == null
                    || !idSeccionFormato.equals(solicitud.getSeccionFormato().getIdSeccionFormato()))) continue;

            porSolicitud.computeIfAbsent(solicitud.getIdSolicitud(), k -> new ArrayList<>()).add(doc);
        }

        List<ReporteAprendizDto> resultado = new ArrayList<>();
        for (List<DocumentoSolicitud> docs : porSolicitud.values()) {
            SolicitudEtapaPractica solicitud = docs.get(0).getSolicitud();
            Usuario aprendiz = solicitud.getAprendizFicha().getUsuario();
            Ficha fichaAprendiz = solicitud.getAprendizFicha().getFicha();

            List<ReporteAprendizDto.DocumentoAprobado> documentos = docs.stream()
                    .sorted(Comparator.comparing(DocumentoSolicitud::getFechaSubida))
                    .map(d -> ReporteAprendizDto.DocumentoAprobado.builder()
                            .nombreDocumento(d.getPlantillaFormato() != null
                                    ? d.getPlantillaFormato().getNombreDocumento() : "Documento adjunto")
                            .asunto(d.getAsunto())
                            .fechaSubida(d.getFechaSubida().format(FORMATO_FECHA))
                            .rutaArchivo(d.getRutaArchivoLleno())
                            .build())
                    .toList();

            resultado.add(ReporteAprendizDto.builder()
                    .idSolicitud(solicitud.getIdSolicitud())
                    .nombre(aprendiz.getNombre())
                    .apellido(aprendiz.getApellido())
                    .tipoDocumento(aprendiz.getTipoDocumento() != null ? aprendiz.getTipoDocumento().name() : "")
                    .documento(aprendiz.getDocumento())
                    .numeroFicha(fichaAprendiz.getNumeroFicha())
                    .programa(fichaAprendiz.getProgramaFormacion().getNombrePrograma())
                    .modalidadContrato(solicitud.getSeccionFormato() != null
                            ? solicitud.getSeccionFormato().getNombreSeccion() : "—")
                    .estadoSolicitud(solicitud.getEstado().name())
                    .documentos(documentos)
                    .build());
        }

        resultado.sort(Comparator.comparing(ReporteAprendizDto::getApellido, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ReporteAprendizDto::getNombre, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim().toLowerCase();
    }
}
