package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.DashboardInstructorDto;
import com.etapa_productiva.kronos.dto.DashboardInstructorDto.BarraGrafico;
import com.etapa_productiva.kronos.dto.DashboardInstructorDto.SegmentoGrafico;
import com.etapa_productiva.kronos.dto.InstructorAprendizDto;
import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.InstructorSeguimiento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 👨‍🏫 Módulo del Instructor de Seguimiento: consolida los aprendices que tiene asignados
 * (vía ASIGNACION_INSTRUCTOR_ETAPA vigente), arma su dashboard (números + gráficas) y
 * exporta el listado a Excel/PDF.
 */
@Service
public class InstructorSeguimientoService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SIN_DATO = "—";

    private static final String[] TITULOS = {
            "Nombres", "Apellidos", "Tipo Documento", "Documento", "Teléfono", "Correo Electrónico",
            "Ficha", "Programa Formación", "Empresa", "Modalidad Contrato",
            "Inicio Etapa", "Fin Etapa", "Estado Etapa"
    };

    // Estados de etapa y su color para las gráficas (orden fijo para la leyenda)
    private static final LinkedHashMap<String, String> COLOR_ESTADO = new LinkedHashMap<>();
    static {
        COLOR_ESTADO.put("EN_PROGRESO", "#D97706");
        COLOR_ESTADO.put("APROBADO", "#057015");
        COLOR_ESTADO.put("REPROBADO", "#DC2626");
        COLOR_ESTADO.put("EN_SUSPENSION", "#6B7280");
    }

    @Autowired
    private InstructorSeguimientoRepository instructorSeguimientoRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    /** Aprendices con asignación vigente del instructor (resuelto desde el usuario logueado). */
    @Transactional(readOnly = true)
    public List<InstructorAprendizDto> listarAprendices(Long idUsuario) {
        InstructorSeguimiento instructor = instructorSeguimientoRepository.findByUsuarioIdUsuario(idUsuario).orElse(null);
        if (instructor == null) {
            return new ArrayList<>();
        }

        List<InstructorAprendizDto> filas = new ArrayList<>();
        for (AsignacionInstructorEtapa asignacion :
                asignacionInstructorEtapaRepository.findByInstructorIdInstructorSeguimientoAndEstadoAsignacionTrue(
                        instructor.getIdInstructorSeguimiento())) {

            EtapaProductiva etapa = asignacion.getEtapaProductiva();
            Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
            Ficha ficha = etapa.getAprendizFicha().getFicha();

            filas.add(InstructorAprendizDto.builder()
                    .nombres(valor(aprendiz.getNombre()))
                    .apellidos(valor(aprendiz.getApellido()))
                    .tipoDocumento(aprendiz.getTipoDocumento() != null ? aprendiz.getTipoDocumento().name() : SIN_DATO)
                    .documento(valor(aprendiz.getDocumento()))
                    .telefono(valor(aprendiz.getTelefono()))
                    .correoElectronico(valor(aprendiz.getCorreoElectronico()))
                    .ficha(ficha.getNumeroFicha())
                    .programaFormacion(ficha.getProgramaFormacion().getNombrePrograma())
                    .razonSocial(etapa.getEmpresa().getNombreEmpresa())
                    .modalidadContrato(etapa.getTipoContrato().getNombreTipoContrato())
                    .etapaInicio(fecha(etapa.getFechaInicio()))
                    .etapaFin(fecha(etapa.getFechaFin()))
                    .estadoEtapa(etapa.getEstadoEtapa() != null ? etapa.getEstadoEtapa().name() : SIN_DATO)
                    .build());
        }

        filas.sort(Comparator.comparing(InstructorAprendizDto::getApellidos, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(InstructorAprendizDto::getNombres, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    /** Calcula números y series de gráficas a partir de la lista de aprendices. */
    public DashboardInstructorDto calcularDashboard(List<InstructorAprendizDto> aprendices) {
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        COLOR_ESTADO.keySet().forEach(e -> porEstado.put(e, 0));
        Map<String, Integer> porFicha = new LinkedHashMap<>();
        java.util.Set<String> empresas = new java.util.HashSet<>();

        for (InstructorAprendizDto a : aprendices) {
            porEstado.merge(a.getEstadoEtapa(), 1, Integer::sum);
            porFicha.merge(a.getFicha(), 1, Integer::sum);
            empresas.add(a.getRazonSocial());
        }

        int total = aprendices.size();

        // Segmentos de la dona (solo estados con al menos 1) + gradiente conic
        List<SegmentoGrafico> segmentos = new ArrayList<>();
        StringBuilder gradiente = new StringBuilder("conic-gradient(");
        double gradoActual = 0;
        boolean primero = true;
        for (Map.Entry<String, String> e : COLOR_ESTADO.entrySet()) {
            int cant = porEstado.getOrDefault(e.getKey(), 0);
            if (cant == 0) continue;
            int pct = total > 0 ? (int) Math.round(cant * 100.0 / total) : 0;
            segmentos.add(SegmentoGrafico.builder()
                    .etiqueta(etiquetaLegible(e.getKey())).cantidad(cant).porcentaje(pct).color(e.getValue()).build());

            double grados = total > 0 ? cant * 360.0 / total : 0;
            if (!primero) gradiente.append(", ");
            gradiente.append(e.getValue()).append(" ").append(round(gradoActual)).append("deg ")
                    .append(round(gradoActual + grados)).append("deg");
            gradoActual += grados;
            primero = false;
        }
        if (total == 0) {
            gradiente.append("#E5E7EB 0deg 360deg");
        }
        gradiente.append(")");

        // Barras: aprendices por ficha (ancho relativo al máximo)
        int maxFicha = porFicha.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        List<BarraGrafico> barras = new ArrayList<>();
        porFicha.forEach((ficha, cant) -> barras.add(BarraGrafico.builder()
                .etiqueta(ficha).cantidad(cant).porcentaje((int) Math.round(cant * 100.0 / maxFicha)).build()));
        barras.sort(Comparator.comparingInt(BarraGrafico::getCantidad).reversed());

        return DashboardInstructorDto.builder()
                .totalAprendices(total)
                .enProgreso(porEstado.getOrDefault("EN_PROGRESO", 0))
                .aprobados(porEstado.getOrDefault("APROBADO", 0))
                .reprobados(porEstado.getOrDefault("REPROBADO", 0))
                .enSuspension(porEstado.getOrDefault("EN_SUSPENSION", 0))
                .totalFichas(porFicha.size())
                .totalEmpresas(empresas.size())
                .donutGradient(gradiente.toString())
                .segmentosEstado(segmentos)
                .aprendicesPorFicha(barras)
                .build();
    }

    public byte[] exportarExcel(List<InstructorAprendizDto> aprendices) throws IOException {
        return ExportacionUtil.excel("Mis Aprendices", TITULOS, filas(aprendices));
    }

    public byte[] exportarPdf(List<InstructorAprendizDto> aprendices) {
        return ExportacionUtil.pdf("KRONOS - Aprendices en Seguimiento", TITULOS, filas(aprendices));
    }

    private List<String[]> filas(List<InstructorAprendizDto> aprendices) {
        List<String[]> filas = new ArrayList<>();
        for (InstructorAprendizDto a : aprendices) {
            filas.add(new String[]{
                    a.getNombres(), a.getApellidos(), a.getTipoDocumento(), a.getDocumento(),
                    a.getTelefono(), a.getCorreoElectronico(), a.getFicha(), a.getProgramaFormacion(),
                    a.getRazonSocial(), a.getModalidadContrato(), a.getEtapaInicio(), a.getEtapaFin(), a.getEstadoEtapa()
            });
        }
        return filas;
    }

    private String etiquetaLegible(String estado) {
        return switch (estado) {
            case "EN_PROGRESO" -> "En progreso";
            case "APROBADO" -> "Aprobado";
            case "REPROBADO" -> "Reprobado";
            case "EN_SUSPENSION" -> "En suspensión";
            default -> estado;
        };
    }

    private long round(double d) {
        return Math.round(d);
    }

    private String fecha(LocalDate f) {
        return f != null ? f.format(FORMATO_FECHA) : SIN_DATO;
    }

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? SIN_DATO : texto;
    }
}
