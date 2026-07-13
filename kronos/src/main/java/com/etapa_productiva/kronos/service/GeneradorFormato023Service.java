package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.EvaluacionMomento;
import com.etapa_productiva.kronos.entity.FactorMomento;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.JuicioEvaluacion;
import com.etapa_productiva.kronos.entity.ModalidadEtapa;
import com.etapa_productiva.kronos.entity.ModalidadFirma;
import com.etapa_productiva.kronos.entity.PlantillaFormato;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.ValoracionFactor;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.DocumentoRequisitoRepository;
import com.etapa_productiva.kronos.repository.EvaluacionMomentoRepository;
import com.etapa_productiva.kronos.repository.FactorMomentoRepository;
import com.etapa_productiva.kronos.repository.PlantillaFormatoRepository;
import com.etapa_productiva.kronos.repository.VisitaSeguimientoRepository;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 📄 Genera el GFPI-F-023 V6 ("Formato de Planeación, Seguimiento y Evaluación de la Etapa
 * Productiva") rellenando una plantilla .docx real del SENA con los datos transaccionales de
 * KRONOS, en vez de dibujar tablas/bordes por código (eso ya lo hace {@link EvaluacionFormatosService}
 * con OpenPDF). La plantilla NUNCA se modifica en disco: se abre en memoria, se reemplazan los
 * placeholders {{clave}} y se devuelve el resultado como bytes.
 *
 * La plantilla debe vivir en {@code src/main/resources/plantillas/GFPI-F-023-V6.docx} y contener
 * los tokens exactos que arma {@link #construirMapaPlaceholders}: si el texto de la plantilla no
 * coincide carácter por carácter con una clave del mapa, ese token queda intacto en el documento
 * final (no lanza error) — hay que revisar la plantilla real contra ese mapa antes de usarla.
 */
@Service
public class GeneradorFormato023Service {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SIN_DATO = "—";
    private static final String MARCA = "X";

    // Slug estable por variable, para armar las claves {{m2_sat_<slug>}} / {{m3_pm_<slug>}} / etc.
    // Debe coincidir 1 a 1 con las 13 filas que siembra EvaluacionFormatosService.FACTORES_TECNICOS
    // + FACTORES_ACTITUDINALES (mismo texto de nombreVariable en FACTOR_MOMENTO).
    private static final LinkedHashMap<String, String> SLUG_POR_VARIABLE = new LinkedHashMap<>();
    static {
        SLUG_POR_VARIABLE.put("Aplicación de conocimiento", "conocimiento");
        SLUG_POR_VARIABLE.put("Mejora continua", "mejora_continua");
        SLUG_POR_VARIABLE.put("Fortalecimiento ocupacional", "fortalecimiento");
        SLUG_POR_VARIABLE.put("Oportunidad y calidad", "oportunidad_calidad");
        SLUG_POR_VARIABLE.put("Responsabilidad ambiental", "resp_ambiental");
        SLUG_POR_VARIABLE.put("Administración de recursos", "admin_recursos");
        SLUG_POR_VARIABLE.put("Seguridad y salud en el trabajo", "seguridad_salud");
        SLUG_POR_VARIABLE.put("Documentación etapa productiva", "documentacion");
        SLUG_POR_VARIABLE.put("Relaciones interpersonales", "relaciones");
        SLUG_POR_VARIABLE.put("Trabajo en equipo", "trabajo_equipo");
        SLUG_POR_VARIABLE.put("Solución de problemas", "solucion_problemas");
        SLUG_POR_VARIABLE.put("Cumplimiento", "cumplimiento");
        SLUG_POR_VARIABLE.put("Organización", "organizacion");
    }

    // Debe coincidir (contains, ignorando mayúsculas) con PLANTILLA_FORMATO.NOMBRE_DOCUMENTO tal
    // como quedó registrada por AdminPlantillasController al subir el archivo real desde el panel
    // de administración — hoy vive en /uploads/plantillas/etapa_practica/Formato Planeacion
    // Seguimiento EP.docx (ver PLANTILLA_FORMATO.ID_PLANTILLA = 5 en el ambiente de desarrollo).
    private static final String NOMBRE_PLANTILLA_023 = "Planeacion Seguimiento";

    @Autowired
    private PlantillaFormatoRepository plantillaFormatoRepository;

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

    // ═══════════════════════════════════ Punto de entrada ═══════════════════════════════════

    /**
     * Genera el .docx del Formato 023 para una Etapa Productiva y lo devuelve en memoria, listo
     * para ser servido como descarga por un controlador HTTP.
     */
    @Transactional(readOnly = true)
    public byte[] generar(EtapaProductiva etapa) throws IOException, InvalidFormatException {
        Map<String, String> valores = construirMapaPlaceholders(etapa);
        Map<String, String> firmas = construirMapaFirmas(etapa);

        PlantillaFormato plantilla = plantillaFormatoRepository
                .findFirstByNombreDocumentoContainingIgnoreCase(NOMBRE_PLANTILLA_023)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una plantilla registrada en PLANTILLA_FORMATO cuyo nombre contenga '"
                                + NOMBRE_PLANTILLA_023 + "'. Súbela primero desde el panel de administración de plantillas."));

        Path rutaFisica = resolverRutaFisica(plantilla.getRutaArchivoPlantilla());
        if (!Files.exists(rutaFisica)) {
            throw new IllegalStateException("La plantilla está registrada en BD pero el archivo físico no existe en: " + rutaFisica);
        }

        try (InputStream entradaPlantilla = Files.newInputStream(rutaFisica);
             XWPFDocument documento = new XWPFDocument(entradaPlantilla)) {

            // REGLA 3: recorrido profundo — párrafos sueltos del documento + todas sus tablas
            // (con recursión dentro de cada celda, por si trae tablas anidadas).
            reemplazarEnParrafos(documento.getParagraphs(), valores);
            for (XWPFTable tabla : documento.getTables()) {
                reemplazarEnTabla(tabla, valores);
            }

            // Firmas: van en un segundo recorrido porque insertan una imagen dentro del run
            // (no es un simple cambio de texto).
            reemplazarFirmasEnParrafos(documento.getParagraphs(), firmas);
            for (XWPFTable tabla : documento.getTables()) {
                reemplazarFirmasEnTabla(tabla, firmas);
            }

            // REGLA 5: retorno seguro en memoria, sin tocar el archivo de plantilla en disco.
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.write(salida);
            return salida.toByteArray();
        }
    }

    // ══════════════════════════ REGLA 2 y 3: motor de reemplazo de texto ══════════════════════════

    private void reemplazarEnParrafos(List<XWPFParagraph> parrafos, Map<String, String> valores) {
        for (XWPFParagraph parrafo : parrafos) {
            reemplazarEnParrafo(parrafo, valores);
        }
    }

    // Recorre filas → celdas de una tabla; por cada celda procesa sus párrafos y, si la celda
    // contiene una tabla anidada (celdas combinadas complejas del GFPI-F-023), se llama a sí misma.
    private void reemplazarEnTabla(XWPFTable tabla, Map<String, String> valores) {
        for (XWPFTableRow fila : tabla.getRows()) {
            for (XWPFTableCell celda : fila.getTableCells()) {
                reemplazarEnParrafos(celda.getParagraphs(), valores);
                for (XWPFTable tablaAnidada : celda.getTables()) {
                    reemplazarEnTabla(tablaAnidada, valores);
                }
            }
        }
    }

    /**
     * REGLA 2 — Word parte una misma etiqueta {@code {{clave}}} en varios XWPFRun (por corrección
     * ortográfica, cambios de idioma, etc.). Este método fusiona el texto de TODOS los runs del
     * párrafo en un solo String, hace el reemplazo ahí, y vuelve a escribir el resultado en un
     * único run (el primero que tenga texto real, para conservar su fuente/tamaño/color nativos),
     * eliminando los runs sobrantes. Si el párrafo no contiene ningún placeholder, se deja
     * exactamente como está — no se toca su formato para nada.
     */
    private void reemplazarEnParrafo(XWPFParagraph parrafo, Map<String, String> valores) {
        List<XWPFRun> runs = parrafo.getRuns();
        if (runs == null || runs.isEmpty()) {
            return;
        }

        StringBuilder textoCompleto = new StringBuilder();
        int indiceRunPrincipal = -1;
        for (int i = 0; i < runs.size(); i++) {
            String textoRun = runs.get(i).getText(0);
            if (textoRun != null) {
                textoCompleto.append(textoRun);
                if (indiceRunPrincipal == -1) {
                    indiceRunPrincipal = i; // primer run con texto real: ahí queda el resultado fusionado
                }
            }
        }

        String textoOriginal = textoCompleto.toString();
        if (indiceRunPrincipal == -1 || !textoOriginal.contains("{{")) {
            return; // sin placeholders (o sin texto, ej. un run que solo trae un salto/imagen)
        }

        String textoReemplazado = textoOriginal;
        for (Map.Entry<String, String> entrada : valores.entrySet()) {
            textoReemplazado = textoReemplazado.replace(entrada.getKey(),
                    entrada.getValue() != null ? entrada.getValue() : "");
        }

        runs.get(indiceRunPrincipal).setText(textoReemplazado, 0);
        for (int i = runs.size() - 1; i >= 0; i--) {
            if (i != indiceRunPrincipal) {
                parrafo.removeRun(i); // de atrás hacia adelante para no invalidar los índices restantes
            }
        }
    }

    // ══════════════════════════════════ Firmas (imágenes) ══════════════════════════════════

    private void reemplazarFirmasEnParrafos(List<XWPFParagraph> parrafos, Map<String, String> firmas)
            throws IOException, InvalidFormatException {
        for (XWPFParagraph parrafo : parrafos) {
            for (XWPFRun run : parrafo.getRuns()) {
                String texto = run.getText(0);
                if (texto == null) {
                    continue;
                }
                String rutaImagen = firmas.get(texto.trim());
                if (rutaImagen != null) {
                    insertarImagenEnRun(run, rutaImagen);
                }
            }
        }
    }

    private void reemplazarFirmasEnTabla(XWPFTable tabla, Map<String, String> firmas)
            throws IOException, InvalidFormatException {
        for (XWPFTableRow fila : tabla.getRows()) {
            for (XWPFTableCell celda : fila.getTableCells()) {
                reemplazarFirmasEnParrafos(celda.getParagraphs(), firmas);
                for (XWPFTable tablaAnidada : celda.getTables()) {
                    reemplazarFirmasEnTabla(tablaAnidada, firmas);
                }
            }
        }
    }

    // Solo maneja el caso real del GFPI-F-023: el placeholder de firma ({{firma_aprendiz}}, etc.)
    // vive solo dentro de su propia celda/run, sin texto adicional alrededor.
    private void insertarImagenEnRun(XWPFRun run, String rutaImagenEnDisco) throws IOException, InvalidFormatException {
        Path ruta = Paths.get(rutaImagenEnDisco.startsWith("/") ? rutaImagenEnDisco.substring(1) : rutaImagenEnDisco);
        if (!Files.exists(ruta)) {
            run.setText("", 0); // firma aún no subida: se deja la celda vacía, sin romper el documento
            return;
        }
        run.setText("", 0);
        int tipoImagen = ruta.toString().toLowerCase().endsWith(".png") ? Document.PICTURE_TYPE_PNG : Document.PICTURE_TYPE_JPEG;
        try (InputStream imagen = Files.newInputStream(ruta)) {
            run.addPicture(imagen, tipoImagen, ruta.getFileName().toString(), Units.toEMU(140), Units.toEMU(55));
        }
    }

    // ═══════════════════════ Construcción de los datos transaccionales de KRONOS ═══════════════════════

    private Map<String, String> construirMapaFirmas(EtapaProductiva etapa) {
        Map<String, String> firmas = new HashMap<>();
        firmas.put("{{firma_aprendiz}}", etapa.getFirmaAprendizRuta());
        firmas.put("{{firma_instructor}}", etapa.getFirmaInstructorRuta());
        firmas.put("{{firma_ente_coformador}}", etapa.getFirmaEnteCoformadorRuta());
        firmas.values().removeIf(java.util.Objects::isNull);
        return firmas;
    }

    private Map<String, String> construirMapaPlaceholders(EtapaProductiva etapa) {
        Map<String, String> valores = new HashMap<>();
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

        // Información general — valores fijos por decisión explícita del negocio (igual que
        // REGIONAL_PREDETERMINADA/CENTRO_PREDETERMINADO/ESTRATEGIA_PREDETERMINADA en EvaluacionFormatosService)
        valores.put("{{regional}}", "Antioquia");
        valores.put("{{centro_formacion}}", "CTAPT");
        valores.put("{{estrategia_formativa}}", "TITULADA");
        valores.put("{{nivel_formacion}}", valor(ficha.getProgramaFormacion().getNivelFormacion().getNombreNivel()));
        valores.put("{{programa_formacion}}", valor(ficha.getProgramaFormacion().getNombrePrograma()));
        valores.put("{{numero_ficha}}", valor(ficha.getNumeroFicha()));
        // La plantilla trae 3 casillas independientes (Presencial/Virtual/A Distancia) para la
        // modalidad de FORMACIÓN del programa — no es lo mismo que ModalidadEtapa (que describe
        // cómo se ejecuta la etapa productiva en sí). Se usa como mejor aproximación disponible;
        // si KRONOS llega a modelar la modalidad académica del programa por separado, ajustar aquí.
        valores.put("{{mod_presencial}}", marca(etapa.getModalidad() == ModalidadEtapa.PRESENCIAL || etapa.getModalidad() == ModalidadEtapa.HIBRIDO));
        valores.put("{{mod_virtual}}", marca(etapa.getModalidad() == ModalidadEtapa.REMOTO || etapa.getModalidad() == ModalidadEtapa.HIBRIDO));
        valores.put("{{mod_distancia}}", "");
        valores.put("{{fecha_fin_etapa_lectiva}}", fecha(ficha.getFechaHabilitacionEtapaPractica().minusDays(1)));

        // Datos del aprendiz
        valores.put("{{aprendiz_nombre_completo}}", aprendiz.getNombre() + " " + aprendiz.getApellido());
        valores.put("{{aprendiz_tipo_documento}}", aprendiz.getTipoDocumento() != null ? aprendiz.getTipoDocumento().name() : SIN_DATO);
        valores.put("{{aprendiz_documento}}", valor(aprendiz.getDocumento()));
        valores.put("{{aprendiz_telefono}}", valor(aprendiz.getTelefono()));
        valores.put("{{aprendiz_direccion}}", SIN_DATO); // KRONOS no guarda dirección del aprendiz hoy
        valores.put("{{aprendiz_correo_personal}}", valor(aprendiz.getCorreoElectronico()));
        valores.put("{{aprendiz_correo_institucional}}", valor(etapa.getCorreoInstitucionalAprendiz()));
        valores.put("{{aprendiz_alternativa_etapa}}", etapa.getTipoContrato().getNombreTipoContrato());
        valores.put("{{aprendiz_fecha_registro_sofia}}",
                etapa.getFechaCreacion() != null ? fecha(etapa.getFechaCreacion().toLocalDate()) : SIN_DATO);

        // Datos del instructor de seguimiento
        Usuario instructor = asignacionInstructorEtapaRepository
                .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(etapa.getIdEtapa())
                .map(a -> a.getInstructor().getUsuario())
                .orElse(null);
        valores.put("{{instructor_nombre}}", instructor != null ? instructor.getNombre() + " " + instructor.getApellido() : SIN_DATO);
        valores.put("{{instructor_telefono}}", instructor != null ? valor(instructor.getTelefono()) : SIN_DATO);
        valores.put("{{instructor_correo}}", SIN_DATO); // sin fuente de dato hoy (igual que en EvaluacionFormatosService)

        // Datos del ente co-formador
        valores.put("{{empresa_nombre}}", etapa.getEmpresa().getNombreEmpresa());
        valores.put("{{empresa_direccion}}", valor(etapa.getEmpresa().getDireccion()));
        valores.put("{{empresa_nit}}", valor(etapa.getEmpresa().getNit()));
        valores.put("{{empresa_correo}}", valor(etapa.getEmpresa().getCorreo()));
        valores.put("{{empresa_jefe_inmediato}}", valor(etapa.getNombreJefeInmediato()));
        valores.put("{{empresa_jefe_cargo}}", SIN_DATO); // sin fuente de dato hoy
        valores.put("{{empresa_telefono_jefe}}", valor(etapa.getTelefonoJefeInmediato()));
        valores.put("{{empresa_otro_contacto}}", SIN_DATO); // sin fuente de dato hoy
        valores.put("{{empresa_telefono_institucional}}", SIN_DATO); // sin fuente de dato hoy

        // Momento 1 — Planeación. La plantilla real solo trae UNA caja de observaciones (no
        // separa aprendiz/instructor como sí hacen los Momentos 2 y 3), así que se combinan.
        valores.put("{{m1_fecha_inicio_etapa}}", fecha(etapa.getFechaInicio()));
        valores.put("{{m1_fecha_fin_etapa}}", fecha(etapa.getFechaFin()));
        valores.put("{{m1_fecha_arl}}", buscarFechaAfiliacionArl(etapa.getIdEtapa()));
        valores.put("{{m1_horario}}", ficha.getJornada() != null ? ficha.getJornada().name() : SIN_DATO);
        valores.put("{{m1_enlace_grabacion}}", m1 != null ? valor(m1.getEnlaceGrabacion()) : SIN_DATO);
        valores.put("{{m1_competencias}}", m1 != null ? valor(m1.getCompetenciasDesarrollar()) : SIN_DATO);
        valores.put("{{m1_resultados}}", m1 != null ? valor(m1.getResultadosAprendizaje()) : SIN_DATO);
        valores.put("{{m1_actividades}}", m1 != null ? valor(m1.getActividadesDesarrollar()) : SIN_DATO);
        valores.put("{{m1_evidencias}}", m1 != null ? valor(m1.getEvidenciaDescripcion()) : SIN_DATO);
        valores.put("{{m1_observaciones}}", combinarObservaciones(
                m1 != null ? m1.getObservacionAprendiz() : null, m1 != null ? m1.getObservacion() : null));
        agregarCierreMomento(valores, "m1", m1);

        // Momento 2 — Seguimiento
        valores.put("{{m2_fecha_inicio_etapa}}", fecha(etapa.getFechaInicio()));
        valores.put("{{m2_fecha_momento_seguimiento}}", m2 != null && m2.getFechaMomento() != null ? fecha(m2.getFechaMomento()) : SIN_DATO);
        valores.put("{{m2_modalidad}}", m2 != null && m2.getModalidadFirma() != null ? etiquetaModalidadFirma(m2.getModalidadFirma()) : SIN_DATO);
        valores.put("{{m2_enlace_grabacion}}", m2 != null ? valor(m2.getEnlaceGrabacion()) : SIN_DATO);
        valores.put("{{m2_obs_instructor}}", m2 != null ? valor(m2.getObservacion()) : SIN_DATO);
        valores.put("{{m2_obs_aprendiz}}", m2 != null ? valor(m2.getObservacionAprendiz()) : SIN_DATO);
        valores.put("{{m2_obs_ente_coformador}}", m2 != null ? valor(m2.getObservacionEnteCoformador()) : SIN_DATO);
        agregarFactores(valores, "m2", m2);
        agregarCierreMomento(valores, "m2", m2);

        // Momento 3 — Evaluación
        long visitasRealizadas = visitaSeguimientoRepository
                .findByEtapaProductivaIdEtapaOrderByFechaVisitaDesc(etapa.getIdEtapa()).stream()
                .filter(v -> v.getEstadoVisita() == EstadoVisita.REALIZADA)
                .count();
        valores.put("{{m3_fecha_inicio_etapa}}", fecha(etapa.getFechaInicio()));
        valores.put("{{m3_fecha_fin_etapa}}", fecha(etapa.getFechaFin()));
        valores.put("{{m3_num_visitas}}", String.valueOf(visitasRealizadas));
        valores.put("{{m3_modalidad}}", m3 != null && m3.getModalidadFirma() != null ? etiquetaModalidadFirma(m3.getModalidadFirma()) : SIN_DATO);
        valores.put("{{m3_enlace_grabacion}}", m3 != null ? valor(m3.getEnlaceGrabacion()) : SIN_DATO);
        valores.put("{{m3_retro_ente_proceso}}", m3 != null ? valor(m3.getRetroEnteProceso()) : SIN_DATO);
        valores.put("{{m3_retro_ente_desempeno}}", m3 != null ? valor(m3.getRetroEnteDesempeno()) : SIN_DATO);
        valores.put("{{m3_retro_instructor_proceso}}", m3 != null ? valor(m3.getRetroInstructorProceso()) : SIN_DATO);
        valores.put("{{m3_retro_instructor_desempeno}}", m3 != null ? valor(m3.getRetroInstructorDesempeno()) : SIN_DATO);
        valores.put("{{m3_retro_aprendiz_proceso}}", m3 != null ? valor(m3.getRetroAprendizProceso()) : SIN_DATO);
        valores.put("{{m3_retro_aprendiz_desempeno}}", m3 != null ? valor(m3.getRetroAprendizDesempeno()) : SIN_DATO);
        valores.put("{{m3_juicio_aprobado}}", marca(m3 != null && m3.getJuicioEvaluacion() == JuicioEvaluacion.APROBADO));
        valores.put("{{m3_juicio_no_aprobado}}", marca(m3 != null && m3.getJuicioEvaluacion() == JuicioEvaluacion.NO_APROBADO));
        agregarFactores(valores, "m3", m3);
        agregarCierreMomento(valores, "m3", m3);

        return valores;
    }

    // REGLA 4 — checkboxes de valoración: por cada una de las 13 variables (8 técnicas + 5
    // actitudinales) deja "X" en la clave que corresponda (satisfactorio/por mejorar) y "" en la
    // otra, más la observación de esa fila. Si el momento no existe todavía, todo queda en blanco
    // (la plantilla se ve intacta, solo sin marcar).
    private void agregarFactores(Map<String, String> valores, String prefijoMomento, EvaluacionMomento momento) {
        if (momento == null || momento.getIdEvaluacionMomento() == null) {
            for (String slug : SLUG_POR_VARIABLE.values()) {
                valores.put("{{" + prefijoMomento + "_sat_" + slug + "}}", "");
                valores.put("{{" + prefijoMomento + "_pm_" + slug + "}}", "");
                valores.put("{{" + prefijoMomento + "_obs_" + slug + "}}", "");
            }
            return;
        }
        for (FactorMomento factor : factorMomentoRepository
                .findByEvaluacionMomentoIdEvaluacionMomentoOrderByIdFactorAsc(momento.getIdEvaluacionMomento())) {
            String slug = SLUG_POR_VARIABLE.get(factor.getNombreVariable());
            if (slug == null) {
                continue; // variable no reconocida (dato inconsistente): se ignora sin romper el llenado
            }
            valores.put("{{" + prefijoMomento + "_sat_" + slug + "}}", marca(factor.getValoracion() == ValoracionFactor.SATISFACTORIO));
            valores.put("{{" + prefijoMomento + "_pm_" + slug + "}}", marca(factor.getValoracion() == ValoracionFactor.POR_MEJORAR));
            valores.put("{{" + prefijoMomento + "_obs_" + slug + "}}", valor(factor.getObservacion()));
        }
    }

    // Cierre "Ciudad____________ y fecha de diligenciamiento: ___/___/____ de forma presencial
    // ___ o virtual ___" de cada momento: la plantilla trae blancos con guion bajo, no casillas
    // de texto — en el .docx hay que borrar esos guiones y escribir el token en su lugar.
    private void agregarCierreMomento(Map<String, String> valores, String prefijoMomento, EvaluacionMomento momento) {
        valores.put("{{" + prefijoMomento + "_ciudad}}", momento != null ? valor(momento.getCiudad()) : SIN_DATO);
        valores.put("{{" + prefijoMomento + "_fecha_diligenciamiento}}",
                momento != null && momento.getFechaDiligenciamiento() != null ? fecha(momento.getFechaDiligenciamiento()) : SIN_DATO);
        valores.put("{{" + prefijoMomento + "_marca_presencial}}",
                marca(momento != null && momento.getModalidadFirma() == ModalidadFirma.PRESENCIAL));
        valores.put("{{" + prefijoMomento + "_marca_virtual}}",
                marca(momento == null || momento.getModalidadFirma() == null || momento.getModalidadFirma() == ModalidadFirma.VIRTUAL));
    }

    // ═══════════════════════════════════════ Helpers ═══════════════════════════════════════

    private String buscarFechaAfiliacionArl(Long idEtapa) {
        return documentoRequisitoRepository.findByEtapaProductivaIdEtapa(idEtapa).stream()
                .filter(d -> d.getPlantillaFormato() != null && d.getPlantillaFormato().getNombreDocumento() != null
                        && d.getPlantillaFormato().getNombreDocumento().toUpperCase().contains("ARL"))
                .map(d -> d.getFechaSubida())
                .filter(f -> f != null)
                .map(f -> f.toLocalDate().format(FORMATO_FECHA))
                .findFirst()
                .orElse(SIN_DATO);
    }

    // Mismo criterio que EvaluacionFormatosService.resolverRutaFisica: las rutas se guardan como
    // "/uploads/..." (web-relative) y se resuelven contra el directorio de trabajo del proceso.
    private Path resolverRutaFisica(String rutaWeb) {
        String relativa = rutaWeb.startsWith("/") ? rutaWeb.substring(1) : rutaWeb;
        return Paths.get(relativa).toAbsolutePath();
    }

    private String etiquetaModalidadFirma(ModalidadFirma modalidad) {
        return modalidad == ModalidadFirma.PRESENCIAL ? "Presencial" : "Virtual";
    }

    private String marca(boolean condicion) {
        return condicion ? MARCA : "";
    }

    private String fecha(LocalDate f) {
        return f != null ? f.format(FORMATO_FECHA) : SIN_DATO;
    }

    private String valor(String texto) {
        return texto != null && !texto.isBlank() ? texto : SIN_DATO;
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
        return combinado.length() > 0 ? combinado.toString() : SIN_DATO;
    }
}
