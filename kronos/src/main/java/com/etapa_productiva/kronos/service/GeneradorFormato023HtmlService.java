package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.EvaluacionMomento3Dto;
import com.etapa_productiva.kronos.dto.Momento1FormatoDto;
import com.etapa_productiva.kronos.dto.VisitaMomento2Dto;
import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.EvaluacionMomento;
import com.etapa_productiva.kronos.entity.FactorMomento;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.ModalidadEtapa;
import com.etapa_productiva.kronos.entity.ModalidadFirma;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.DocumentoRequisitoRepository;
import com.etapa_productiva.kronos.repository.EvaluacionMomentoRepository;
import com.etapa_productiva.kronos.repository.FactorMomentoRepository;
import com.etapa_productiva.kronos.repository.VisitaSeguimientoRepository;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📄 Genera el GFPI-F-023 en PDF a partir de la plantilla HTML `fragments/PlantillaPlaneacion023`
 * (réplica visual del formato real: tablas, negrillas, encabezados en negro, checkboxes). Renderiza
 * el HTML con Thymeleaf y lo convierte a PDF con openhtmltopdf; Jsoup actúa de puente (parsea el
 * HTML de forma tolerante, como un navegador) porque openhtmltopdf exige XML estricto.
 */
@Service
public class GeneradorFormato023HtmlService {

    private static final DateTimeFormatter FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter FORMATO_ANIO = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Mismo orden estable de siembra que EvaluacionFormatosService.FACTORES_TECNICOS/ACTITUDINALES
    private static final String[] FACTORES_TECNICOS = {
            "Aplicación de conocimiento", "Mejora continua", "Fortalecimiento ocupacional",
            "Oportunidad y calidad", "Responsabilidad ambiental", "Administración de recursos",
            "Seguridad y salud en el trabajo", "Documentación etapa productiva"
    };
    private static final String[] FACTORES_ACTITUDINALES = {
            "Relaciones interpersonales", "Trabajo en equipo", "Solución de problemas",
            "Cumplimiento", "Organización"
    };

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private EvaluacionMomentoRepository evaluacionMomentoRepository;

    @Autowired
    private FactorMomentoRepository factorMomentoRepository;

    @Autowired
    private VisitaSeguimientoRepository visitaSeguimientoRepository;

    @Autowired
    private DocumentoRequisitoRepository documentoRequisitoRepository;

    private String logoBase64;

    @PostConstruct
    private void cargarLogo() throws IOException {
        try (InputStream logo = new ClassPathResource("static/images/logo-sena.png").getInputStream()) {
            logoBase64 = Base64.getEncoder().encodeToString(logo.readAllBytes());
        }
    }

    // ═══════════════════════════════════ Punto de entrada ═══════════════════════════════════

    @Transactional(readOnly = true)
    public byte[] generarPdf(EtapaProductiva etapa) throws IOException {
        return renderizarPdf(construirContexto(etapa, null));
    }

    /**
     * 📄 Genera el PDF de un único momento (1, 2 o 3): mismo encabezado general (datos del
     * aprendiz, empresa e instructor) pero solo la tabla de ese momento, como "plus" para que
     * el Instructor de Seguimiento pueda descargarlo apenas quede completo por ambos lados,
     * sin esperar a que los 3 estén listos para el Formato 023 completo.
     */
    @Transactional(readOnly = true)
    public byte[] generarPdfMomento(EtapaProductiva etapa, int numeroMomento) throws IOException {
        return renderizarPdf(construirContexto(etapa, numeroMomento));
    }

    private byte[] renderizarPdf(Context contexto) throws IOException {
        String html = templateEngine.process("fragments/PlantillaPlaneacion023", contexto);

        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
        jsoupDoc.outputSettings().syntax(Syntax.xml);
        org.w3c.dom.Document documentoW3c = new W3CDom().fromJsoup(jsoupDoc);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withW3cDocument(documentoW3c, "");
        builder.toStream(salida);
        builder.run();
        return salida.toByteArray();
    }

    // ═══════════════════ Construcción de los datos transaccionales de KRONOS ═══════════════════

    // soloMomento null = los 3 momentos (Formato 023 completo); 1/2/3 = solo la tabla de ese momento
    private Context construirContexto(EtapaProductiva etapa, Integer soloMomento) {
        Context contexto = new Context();
        contexto.setVariable("logoBase64", logoBase64);
        contexto.setVariable("soloMomento", soloMomento);

        Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
        Ficha ficha = etapa.getAprendizFicha().getFicha();

        Map<Integer, EvaluacionMomento> porNumero = new HashMap<>();
        for (EvaluacionMomento m : evaluacionMomentoRepository
                .findByEtapaProductivaIdEtapaOrderByNumeroMomentoAsc(etapa.getIdEtapa())) {
            porNumero.put(m.getNumeroMomento(), m);
        }
        EvaluacionMomento m1 = porNumero.get(1);
        EvaluacionMomento m2 = porNumero.get(2);
        EvaluacionMomento m3 = porNumero.get(3);

        // Información general (valores fijos por decisión del negocio, igual que en EvaluacionFormatosService)
        contexto.setVariable("regional", "Antioquia");
        contexto.setVariable("centroFormacion", "CTAPT");
        contexto.setVariable("estrategiaFormativa", "TITULADA");
        contexto.setVariable("nivelFormativo", valor(ficha.getProgramaFormacion().getNivelFormacion().getNombreNivel()));
        contexto.setVariable("programaFormacion", valor(ficha.getProgramaFormacion().getNombrePrograma()));
        contexto.setVariable("numeroGrupo", valor(ficha.getNumeroFicha()));
        // La modalidad de FORMACIÓN del programa (Presencial/Virtual/A Distancia) no es lo mismo
        // que ModalidadEtapa (cómo se ejecuta la etapa productiva) — se usa como mejor aproximación.
        contexto.setVariable("modPresencial", marca(etapa.getModalidad() == ModalidadEtapa.PRESENCIAL || etapa.getModalidad() == ModalidadEtapa.HIBRIDO));
        contexto.setVariable("modVirtual", marca(etapa.getModalidad() == ModalidadEtapa.REMOTO || etapa.getModalidad() == ModalidadEtapa.HIBRIDO));
        contexto.setVariable("modDistancia", "");
        contexto.setVariable("fechaFinEtapaLectiva", fecha(ficha.getFechaHabilitacionEtapaPractica().minusDays(1)));

        // Datos del aprendiz
        contexto.setVariable("aprendizNombreCompleto", aprendiz.getNombre() + " " + aprendiz.getApellido());
        contexto.setVariable("aprendizTipoDocumento", aprendiz.getTipoDocumento() != null ? aprendiz.getTipoDocumento().name() : "");
        contexto.setVariable("aprendizDocumento", valor(aprendiz.getDocumento()));
        contexto.setVariable("aprendizTelefono", valor(aprendiz.getTelefono()));
        contexto.setVariable("aprendizDireccion", ""); // KRONOS no guarda dirección del aprendiz hoy
        contexto.setVariable("aprendizCorreoPersonal", valor(aprendiz.getCorreoElectronico()));
        contexto.setVariable("aprendizCorreoInstitucional", valor(etapa.getCorreoInstitucionalAprendiz()));
        contexto.setVariable("aprendizAlternativaEtapa", etapa.getTipoContrato().getNombreTipoContrato());
        contexto.setVariable("aprendizFechaRegistroSofia",
                etapa.getFechaCreacion() != null ? fecha(etapa.getFechaCreacion().toLocalDate()) : "");

        // Datos del instructor de seguimiento
        Usuario instructor = asignacionInstructorEtapaRepository
                .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(etapa.getIdEtapa())
                .map(a -> a.getInstructor().getUsuario())
                .orElse(null);
        contexto.setVariable("instructorNombre", instructor != null ? instructor.getNombre() + " " + instructor.getApellido() : "");
        contexto.setVariable("instructorTelefono", instructor != null ? valor(instructor.getTelefono()) : "");
        contexto.setVariable("instructorCorreo", ""); // sin fuente de dato hoy

        // Datos del ente co-formador
        contexto.setVariable("empresaNombre", etapa.getEmpresa().getNombreEmpresa());
        contexto.setVariable("empresaDireccion", valor(etapa.getEmpresa().getDireccion()));
        contexto.setVariable("empresaNit", valor(etapa.getEmpresa().getNit()));
        contexto.setVariable("empresaCorreo", valor(etapa.getEmpresa().getCorreo()));
        contexto.setVariable("empresaJefeInmediato", valor(etapa.getNombreJefeInmediato()));
        contexto.setVariable("empresaCargo", ""); // sin fuente de dato hoy
        contexto.setVariable("empresaTelefonoJefe", valor(etapa.getTelefonoJefeInmediato()));
        contexto.setVariable("empresaOtroContacto", ""); // sin fuente de dato hoy
        contexto.setVariable("empresaTelefonoInstitucional", ""); // sin fuente de dato hoy

        // Firmas (imagen) subidas en KRONOS: se capturan una sola vez por etapa y la plantilla
        // las reutiliza en los bloques de firma de los 3 momentos. Si alguna falta, queda null
        // y la plantilla deja solo la línea en blanco (para firmar a mano).
        contexto.setVariable("firmaAprendiz", firmaDataUri(etapa.getFirmaAprendizRuta()));
        contexto.setVariable("firmaInstructor", firmaDataUri(etapa.getFirmaInstructorRuta()));
        contexto.setVariable("firmaEnteCoformador", firmaDataUri(etapa.getFirmaEnteCoformadorRuta()));

        contexto.setVariable("momento1", construirMomento1(etapa, m1));
        contexto.setVariable("visita", construirVisita(etapa, m2));
        contexto.setVariable("evaluacion", construirEvaluacion(etapa, m3));

        return contexto;
    }

    private Momento1FormatoDto construirMomento1(EtapaProductiva etapa, EvaluacionMomento m1) {
        Momento1FormatoDto.Momento1FormatoDtoBuilder dto = Momento1FormatoDto.builder()
                .fechaInicio(fecha(etapa.getFechaInicio()))
                .fechaFin(fecha(etapa.getFechaFin()))
                .fechaArl(buscarFechaAfiliacionArl(etapa.getIdEtapa()))
                .numeroPoliza("")
                .horario(m1 != null && etapa.getAprendizFicha().getFicha().getJornada() != null
                        ? etapa.getAprendizFicha().getFicha().getJornada().name() : "");

        if (m1 != null) {
            dto.enlaceGrabacion(valor(m1.getEnlaceGrabacion()))
               .competencias(valor(m1.getCompetenciasDesarrollar()))
               .resultados(valor(m1.getResultadosAprendizaje()))
               .actividades(valor(m1.getActividadesDesarrollar()))
               .evidencias(valor(m1.getEvidenciaDescripcion()))
               .observaciones(combinarObservaciones(m1.getObservacionAprendiz(), m1.getObservacion()));
        }
        aplicarCierre(dto::ciudad, dto::fechaDia, dto::fechaMes, dto::fechaAnio, dto::modalidadCierre, m1);
        return dto.build();
    }

    private VisitaMomento2Dto construirVisita(EtapaProductiva etapa, EvaluacionMomento m2) {
        VisitaMomento2Dto.VisitaMomento2DtoBuilder dto = VisitaMomento2Dto.builder()
                .fechaInicio(fecha(etapa.getFechaInicio()));

        if (m2 != null) {
            dto.fechaSeguimiento(m2.getFechaMomento() != null ? fecha(m2.getFechaMomento()) : "")
               .modalidad(m2.getModalidadFirma() != null ? etiquetaModalidadFirma(m2.getModalidadFirma()) : "")
               .enlaceGrabacion(valor(m2.getEnlaceGrabacion()))
               .obsComplementariasInstructor(valor(m2.getObservacion()))
               .obsAprendiz(valor(m2.getObservacionAprendiz()))
               .obsCoformador(valor(m2.getObservacionEnteCoformador()));
        }

        List<FactorMomento> factores = obtenerFactoresOrdenados(m2);
        Object[] valores = new Object[26];
        for (int i = 0; i < factores.size() && i < 13; i++) {
            FactorMomento f = factores.get(i);
            valores[i * 2] = valorFactor(f);
            valores[i * 2 + 1] = f.getObservacion();
        }
        dto.aplicacionConocimiento((String) valores[0]).obsConocimiento((String) valores[1])
           .mejoraContinua((String) valores[2]).obsMejora((String) valores[3])
           .fortalecimiento((String) valores[4]).obsFortalecimiento((String) valores[5])
           .oportunidad((String) valores[6]).obsOportunidad((String) valores[7])
           .responsabilidad((String) valores[8]).obsResponsabilidad((String) valores[9])
           .administracion((String) valores[10]).obsAdministracion((String) valores[11])
           .seguridad((String) valores[12]).obsSeguridad((String) valores[13])
           .documentacion((String) valores[14]).obsDocumentacion((String) valores[15])
           .relaciones((String) valores[16]).obsRelaciones((String) valores[17])
           .equipo((String) valores[18]).obsEquipo((String) valores[19])
           .solucion((String) valores[20]).obsSolucion((String) valores[21])
           .cumplimiento((String) valores[22]).obsCumplimiento((String) valores[23])
           .organizacion((String) valores[24]).obsOrganizacion((String) valores[25]);

        aplicarCierre(dto::ciudad, dto::fechaDia, dto::fechaMes, dto::fechaAnio, dto::modalidadCierre, m2);
        return dto.build();
    }

    private EvaluacionMomento3Dto construirEvaluacion(EtapaProductiva etapa, EvaluacionMomento m3) {
        long visitasRealizadas = visitaSeguimientoRepository
                .findByEtapaProductivaIdEtapaOrderByFechaVisitaDesc(etapa.getIdEtapa()).stream()
                .filter(v -> v.getEstadoVisita() == EstadoVisita.REALIZADA)
                .count();

        EvaluacionMomento3Dto.EvaluacionMomento3DtoBuilder dto = EvaluacionMomento3Dto.builder()
                .fechaInicio(fecha(etapa.getFechaInicio()))
                .fechaFin(fecha(etapa.getFechaFin()))
                .numeroVisitas(String.valueOf(visitasRealizadas));

        if (m3 != null) {
            dto.modalidad(m3.getModalidadFirma() != null ? etiquetaModalidadFirma(m3.getModalidadFirma()) : "")
               .enlaceGrabacion(valor(m3.getEnlaceGrabacion()))
               .retroProcesoCoformador(valor(m3.getRetroEnteProceso()))
               .retroDesempenoCoformador(valor(m3.getRetroEnteDesempeno()))
               .retroProcesoInstructor(valor(m3.getRetroInstructorProceso()))
               .retroDesempenoInstructor(valor(m3.getRetroInstructorDesempeno()))
               .retroProcesoAprendiz(valor(m3.getRetroAprendizProceso()))
               .retroDesempenoAprendiz(valor(m3.getRetroAprendizDesempeno()))
               .juicioFinal(m3.getJuicioEvaluacion() != null ? m3.getJuicioEvaluacion().name() : "");
        }

        List<FactorMomento> factores = obtenerFactoresOrdenados(m3);
        Object[] valores = new Object[26];
        for (int i = 0; i < factores.size() && i < 13; i++) {
            FactorMomento f = factores.get(i);
            valores[i * 2] = valorFactor(f);
            valores[i * 2 + 1] = f.getObservacion();
        }
        dto.aplicacionConocimiento((String) valores[0]).obsConocimiento((String) valores[1])
           .mejoraContinua((String) valores[2]).obsMejora((String) valores[3])
           .fortalecimiento((String) valores[4]).obsFortalecimiento((String) valores[5])
           .oportunidad((String) valores[6]).obsOportunidad((String) valores[7])
           .responsabilidad((String) valores[8]).obsResponsabilidad((String) valores[9])
           .administracion((String) valores[10]).obsAdministracion((String) valores[11])
           .seguridad((String) valores[12]).obsSeguridad((String) valores[13])
           .documentacion((String) valores[14]).obsDocumentacion((String) valores[15])
           .relaciones((String) valores[16]).obsRelaciones((String) valores[17])
           .equipo((String) valores[18]).obsEquipo((String) valores[19])
           .solucion((String) valores[20]).obsSolucion((String) valores[21])
           .cumplimiento((String) valores[22]).obsCumplimiento((String) valores[23])
           .organizacion((String) valores[24]).obsOrganizacion((String) valores[25]);

        aplicarCierre(dto::ciudad, dto::fechaDia, dto::fechaMes, dto::fechaAnio, dto::modalidadCierre, m3);
        return dto.build();
    }

    // Los 8 Técnicos + 5 Actitudinales se siembran siempre en el mismo orden (ver FACTORES_TECNICOS
    // /FACTORES_ACTITUDINALES en EvaluacionFormatosService), así que se pueden mapear por posición.
    private List<FactorMomento> obtenerFactoresOrdenados(EvaluacionMomento momento) {
        if (momento == null || momento.getIdEvaluacionMomento() == null) {
            return List.of();
        }
        return factorMomentoRepository
                .findByEvaluacionMomentoIdEvaluacionMomentoOrderByIdFactorAsc(momento.getIdEvaluacionMomento());
    }

    private String valorFactor(FactorMomento factor) {
        return factor.getValoracion() != null ? factor.getValoracion().name() : null;
    }

    // Interfaces funcionales locales para reutilizar la misma lógica de cierre en los 3 DTOs
    // (cada builder expone un setter encadenable con la misma firma "this").
    private interface AsignadorTexto<T> {
        T aplicar(String valor);
    }

    private <T> void aplicarCierre(AsignadorTexto<T> ciudad, AsignadorTexto<T> dia, AsignadorTexto<T> mes,
            AsignadorTexto<T> anio, AsignadorTexto<T> modalidadCierre, EvaluacionMomento momento) {
        ciudad.aplicar(momento != null ? valor(momento.getCiudad()) : "");
        LocalDate fecha = momento != null ? momento.getFechaDiligenciamiento() : null;
        dia.aplicar(fecha != null ? fecha.format(FORMATO_DIA_MES) : "");
        mes.aplicar(fecha != null ? fecha.format(FORMATO_MES) : "");
        anio.aplicar(fecha != null ? fecha.format(FORMATO_ANIO) : "");
        modalidadCierre.aplicar(momento != null && momento.getModalidadFirma() == ModalidadFirma.PRESENCIAL ? "Presencial" : "Virtual");
    }

    // ═══════════════════════════════════════ Helpers ═══════════════════════════════════════

    // Convierte la ruta web de una firma (ej. "/uploads/firmas/etapa_1/uuid.png") en un data URI
    // Base64 incrustable en el PDF — mismo mecanismo que el logo del SENA en cargarLogo().
    // La ruta se resuelve igual que EvaluacionFormatosService.resolverRutaFisica.
    private String firmaDataUri(String rutaWeb) {
        if (rutaWeb == null || rutaWeb.isBlank()) {
            return null;
        }
        try {
            String relativa = rutaWeb.startsWith("/") ? rutaWeb.substring(1) : rutaWeb;
            Path archivo = Paths.get(relativa).toAbsolutePath();
            if (!Files.exists(archivo)) {
                return null;
            }
            String mime = Files.probeContentType(archivo);
            if (mime == null || !mime.startsWith("image/")) {
                mime = "image/png";
            }
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(archivo));
        } catch (IOException e) {
            return null; // firma ilegible: el bloque queda con la línea en blanco, como antes
        }
    }

    private String buscarFechaAfiliacionArl(Long idEtapa) {
        return documentoRequisitoRepository.findByEtapaProductivaIdEtapa(idEtapa).stream()
                .filter(d -> d.getPlantillaFormato() != null && d.getPlantillaFormato().getNombreDocumento() != null
                        && d.getPlantillaFormato().getNombreDocumento().toUpperCase().contains("ARL"))
                .map(d -> d.getFechaSubida())
                .filter(f -> f != null)
                .map(f -> f.toLocalDate().format(FORMATO_FECHA))
                .findFirst()
                .orElse("");
    }

    private String etiquetaModalidadFirma(ModalidadFirma modalidad) {
        return modalidad == ModalidadFirma.PRESENCIAL ? "Presencial" : "Virtual";
    }

    private String marca(boolean condicion) {
        return condicion ? "X" : "";
    }

    private String fecha(LocalDate f) {
        return f != null ? f.format(FORMATO_FECHA) : "";
    }

    private String valor(String texto) {
        return texto != null ? texto : "";
    }

    // Momento 1 solo trae una caja de "Observaciones adicionales" en la plantilla real (a
    // diferencia de Momento 2/3, que sí separan aprendiz/instructor) — se combinan con etiqueta.
    private String combinarObservaciones(String observacionAprendiz, String observacionInstructor) {
        StringBuilder combinado = new StringBuilder();
        if (observacionAprendiz != null && !observacionAprendiz.isBlank()) {
            combinado.append("Aprendiz: ").append(observacionAprendiz.trim());
        }
        if (observacionInstructor != null && !observacionInstructor.isBlank()) {
            if (combinado.length() > 0) combinado.append("  |  ");
            combinado.append("Instructor: ").append(observacionInstructor.trim());
        }
        return combinado.toString();
    }
}
