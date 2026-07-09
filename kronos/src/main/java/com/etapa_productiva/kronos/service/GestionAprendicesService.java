package com.etapa_productiva.kronos.service;


import com.etapa_productiva.kronos.dto.AprendizGestionDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.DocumentoRequisito;
import com.etapa_productiva.kronos.entity.Empresa;
import com.etapa_productiva.kronos.entity.EstadoEtapaAprendiz;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.PlantillaFormato;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.VisibilidadDocumento;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.DocumentoRequisitoRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.PlantillaFormatoRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 🎓 Módulo "Gestión Aprendices" del Gestor de Etapa: consolida la información completa
 * de cada aprendiz matriculado (instructor de seguimiento, datos personales y de programa,
 * empresa coformadora, modalidad de contrato y ARL), la exporta a Excel/PDF y permite
 * subir el soporte de la ARL guardándolo como DocumentoRequisito de la etapa.
 */
@Service
public class GestionAprendicesService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIN_DATO = "—";
    private static final String NOMBRE_PLANTILLA_ARL = "ARL";
    private static final Set<String> EXTENSIONES_ARL = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".doc", ".docx");

    // Mismo orden de la tabla: 1º instructor, 2º aprendiz y su programa, 3º empresa y modalidad
    private static final String[] TITULOS = {
            "Instructor Seguimiento",
            "Tipo Documento", "Documento", "Apellidos", "Nombres", "Teléfono", "Correo Electrónico",
            "Nivel Formación", "Programa Formación", "Ficha", "Fecha Fin Ficha",
            "Razón Social", "Municipio Empresa", "Departamento Empresa", "Correo Empresa",
            "Teléfono Empresa", "Modalidad Contrato", "Contrato Inicio", "Contrato Fin", "ARL", "Registrada el", "Estado Etapa Práctica"
    };

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private DocumentoRequisitoRepository documentoRequisitoRepository;

    @Autowired
    private PlantillaFormatoRepository plantillaFormatoRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    @Transactional(readOnly = true)
    public List<AprendizGestionDto> listarAprendices() {
        List<AprendizGestionDto> filas = new ArrayList<>();

        // 🕐 Orden de llegada: la matrícula (APRENDIZ_FICHA) con ID más alto es la más reciente,
        // así el Gestor ve primero a los aprendices que acaban de ingresar al sistema.
        List<AprendizFicha> matriculas = new ArrayList<>(aprendizFichaRepository.findAll());
        matriculas.sort(Comparator.comparing(AprendizFicha::getIdAprendizFicha).reversed());

        for (AprendizFicha matricula : matriculas) {
            Usuario usuario = matricula.getUsuario();
            Ficha ficha = matricula.getFicha();
            EtapaProductiva etapa = etapaProductivaRepository
                    .findByAprendizIdUsuario(usuario.getIdUsuario())
                    .orElse(null);

            String instructor = "Sin asignar";
            String razonSocial = SIN_DATO;
            String municipioEmpresa = SIN_DATO;
            String departamentoEmpresa = SIN_DATO;
            String correoEmpresa = SIN_DATO;
            String telefonoEmpresa = SIN_DATO;
            String modalidadContrato = SIN_DATO;
            String contratoInicio = SIN_DATO;
            String contratoFin = SIN_DATO;
            String arl = "Sin etapa";
            String arlRuta = null;
            String registradoPor = SIN_DATO;
            String fechaRegistro = SIN_DATO;
            String mesRegistro = null;
            String nitEmpresa = null;
            String jefeNombre = null;
            String jefeCorreo = null;
            String jefeTelefono = null;
            String estadoEtapaEtapa = null;
            String contratoInicioIso = null;
            String contratoFinIso = null;

            if (etapa != null) {
                Empresa empresa = etapa.getEmpresa();
                razonSocial = empresa.getNombreEmpresa();
                municipioEmpresa = empresa.getMunicipio().getNombreMunicipio();
                departamentoEmpresa = empresa.getMunicipio().getDepartamento().getNombreDepartamento();
                correoEmpresa = valor(empresa.getCorreo());
                telefonoEmpresa = valor(empresa.getTelefono());
                modalidadContrato = etapa.getTipoContrato().getNombreTipoContrato();
                contratoInicio = formatear(etapa.getFechaInicio());
                contratoFin = formatear(etapa.getFechaFin());

                nitEmpresa = empresa.getNit();
                jefeNombre = etapa.getNombreJefeInmediato();
                jefeCorreo = etapa.getCorreoJefeInmediato();
                jefeTelefono = etapa.getTelefonoJefeInmediato();
                estadoEtapaEtapa = etapa.getEstadoEtapa() != null ? etapa.getEstadoEtapa().name() : null;
                contratoInicioIso = etapa.getFechaInicio() != null ? etapa.getFechaInicio().toString() : null;
                contratoFinIso = etapa.getFechaFin() != null ? etapa.getFechaFin().toString() : null;
                instructor = asignacionInstructorEtapaRepository
                        .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(etapa.getIdEtapa())
                        .map(a -> a.getInstructor().getUsuario().getNombre() + " " + a.getInstructor().getUsuario().getApellido())
                        .orElse("Sin asignar");

                DocumentoRequisito documentoArl = buscarDocumentoArl(etapa.getIdEtapa());
                if (documentoArl != null) {
                    arl = documentoArl.getEstadoValidacion().name();
                    arlRuta = documentoArl.getRutaArchivoLleno();
                } else {
                    arl = "Sin cargar";
                }

                if (etapa.getUsuarioRegistro() != null) {
                    registradoPor = etapa.getUsuarioRegistro().getNombre() + " " + etapa.getUsuarioRegistro().getApellido();
                }
                if (etapa.getFechaCreacion() != null) {
                    fechaRegistro = etapa.getFechaCreacion().format(FORMATO_FECHA_HORA);
                    mesRegistro = com.etapa_productiva.kronos.util.FechaUtil.claveMes(etapa.getFechaCreacion());
                }
            }

            EstadoEtapaAprendiz estadoEtapa = calcularEstadoEtapa(etapa, ficha);
            String modalidadEtapa = calcularModalidad(etapa, matricula);

            filas.add(AprendizGestionDto.builder()
                    .instructorSeguimiento(instructor)
                    .tipoDocumento(usuario.getTipoDocumento() != null ? usuario.getTipoDocumento().name() : SIN_DATO)
                    .documento(valor(usuario.getDocumento()))
                    .apellidos(valor(usuario.getApellido()))
                    .nombres(valor(usuario.getNombre()))
                    .telefono(valor(usuario.getTelefono()))
                    .correoElectronico(valor(usuario.getCorreoElectronico()))
                    .nivelFormacion(ficha.getProgramaFormacion().getNivelFormacion().getNombreNivel())
                    .programaFormacion(ficha.getProgramaFormacion().getNombrePrograma())
                    .ficha(ficha.getNumeroFicha())
                    .fechaFinFicha(formatear(ficha.getFechaFin()))
                    .razonSocial(razonSocial)
                    .municipioEmpresa(municipioEmpresa)
                    .departamentoEmpresa(departamentoEmpresa)
                    .correoEmpresa(correoEmpresa)
                    .telefonoEmpresa(telefonoEmpresa)
                    .modalidadContrato(modalidadContrato)
                    .contratoInicio(contratoInicio)
                    .contratoFin(contratoFin)
                    .arl(arl)
                    .arlRuta(arlRuta)
                    .registradoPor(registradoPor)
                    .fechaRegistro(fechaRegistro)
                    .mesRegistro(mesRegistro)
                    .nitEmpresa(nitEmpresa)
                    .jefeNombre(jefeNombre)
                    .jefeCorreo(jefeCorreo)
                    .jefeTelefono(jefeTelefono)
                    .estadoEtapaEtapa(estadoEtapaEtapa)
                    .contratoInicioIso(contratoInicioIso)
                    .contratoFinIso(contratoFinIso)
                    .idEtapa(etapa != null ? etapa.getIdEtapa() : null)
                    .estadoEtapa(estadoEtapa)
                    .estadoEtapaTexto(construirTextoEstado(estadoEtapa, modalidadEtapa))
                    .build());
        }

        // Las filas ya vienen en orden de llegada (más reciente → más antiguo); no se reordena.
        return filas;
    }

    /**
     * 🚦 Clasificación semáforo (formato SENA): si ya tiene Etapa Productiva, compara su
     * FECHA_FIN contra hoy (terminada / a un mes o menos de terminar / en progreso). Si aún
     * no la tiene, usa la fecha de habilitación de la ficha (6 meses antes de su fin) para
     * saber si está a un mes o menos de poder iniciarla o si todavía le falta más tiempo.
     */
    private EstadoEtapaAprendiz calcularEstadoEtapa(EtapaProductiva etapa, Ficha ficha) {
        LocalDate hoy = LocalDate.now();
        if (etapa != null) {
            LocalDate fin = etapa.getFechaFin();
            if (hoy.isAfter(fin)) {
                return EstadoEtapaAprendiz.TERMINO_CONTRATO;
            }
            return dentroDeUnMes(fin, hoy) ? EstadoEtapaAprendiz.EP_POR_TERMINAR : EstadoEtapaAprendiz.EN_EP;
        }
        LocalDate fechaHabilitacion = ficha.getFechaHabilitacionEtapaPractica();
        return dentroDeUnMes(fechaHabilitacion, hoy) ? EstadoEtapaAprendiz.INICIA_EP_PRONTO : EstadoEtapaAprendiz.FALTA_MAS_DE_UN_MES;
    }

    private boolean dentroDeUnMes(LocalDate fechaObjetivo, LocalDate hoy) {
        return !fechaObjetivo.isAfter(hoy.plusMonths(1));
    }

    /**
     * 🏷️ El texto del semáforo depende de la modalidad de contrato que el aprendiz eligió:
     * si ya tiene Etapa Productiva, la modalidad viene de su TipoContrato registrado; si aún
     * no la tiene, se toma del SeccionFormato de su solicitud (la modalidad a la que aspira).
     */
    private String calcularModalidad(EtapaProductiva etapa, AprendizFicha matricula) {
        if (etapa != null && etapa.getTipoContrato() != null) {
            return normalizarModalidad(etapa.getTipoContrato().getNombreTipoContrato());
        }
        SolicitudEtapaPractica solicitud = solicitudRepository
                .findByAprendizFichaIdAprendizFicha(matricula.getIdAprendizFicha())
                .orElse(null);
        if (solicitud != null && solicitud.getSeccionFormato() != null) {
            return normalizarModalidad(solicitud.getSeccionFormato().getNombreSeccion());
        }
        return "Etapa Práctica";
    }

    // El catálogo de modalidades no tiene una capitalización consistente ("Pasantia",
    // "vinculo formativo", "MONITORIA"...), así que se normaliza para verse profesional.
    private String normalizarModalidad(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "Etapa Práctica";
        }
        String[] palabras = nombre.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (String palabra : palabras) {
            if (!resultado.isEmpty()) {
                resultado.append(' ');
            }
            resultado.append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1));
        }
        return resultado.toString();
    }

    // Arma el texto final del semáforo, nombrando la modalidad real del aprendiz en vez de
    // un genérico "contrato de aprendizaje" que no aplica igual a pasantías, monitorías, etc.
    private String construirTextoEstado(EstadoEtapaAprendiz estado, String modalidad) {
        return switch (estado) {
            case TERMINO_CONTRATO -> "Terminó " + modalidad;
            case EP_POR_TERMINAR -> "En " + modalidad + ": termina en ≤1 mes";
            case EN_EP -> "En " + modalidad;
            case INICIA_EP_PRONTO -> "Inicia " + modalidad + " en ≤1 mes";
            case FALTA_MAS_DE_UN_MES -> "A +1 mes de iniciar " + modalidad;
        };
    }

    /**
     * 📎 Guarda (o reemplaza) el soporte de la ARL de una Etapa Productiva como
     * DocumentoRequisito. Al subirlo el propio Gestor de Etapa —quien valida los
     * documentos— queda directamente APROBADO.
     */
    @Transactional
    public void subirArl(Long idEtapa, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar el archivo de la ARL.");
        }

        EtapaProductiva etapa = etapaProductivaRepository.findById(idEtapa)
                .orElseThrow(() -> new IllegalArgumentException("La Etapa Productiva indicada no existe."));

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
        int puntoIdx = nombreOriginal.lastIndexOf('.');
        String extension = puntoIdx >= 0 ? nombreOriginal.substring(puntoIdx).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSIONES_ARL.contains(extension)) {
            throw new IllegalArgumentException("La ARL debe ser un PDF, imagen (JPG/PNG) o documento Word.");
        }

        // La plantilla "ARL" es el ancla del requisito; si el catálogo aún no la tiene, se crea
        PlantillaFormato plantillaArl = plantillaFormatoRepository
                .findFirstByNombreDocumentoContainingIgnoreCase(NOMBRE_PLANTILLA_ARL)
                .orElseGet(() -> plantillaFormatoRepository.save(PlantillaFormato.builder()
                        .nombreDocumento(NOMBRE_PLANTILLA_ARL)
                        .rutaArchivoPlantilla("N/A") // requisito administrativo: no existe plantilla física para la ARL
                        .visibilidad(VisibilidadDocumento.SOLO_COORDINADOR)
                        .build()));

        String rutaWeb;
        try {
            Path directorio = Paths.get(uploadRootDir, "requisitos", "etapa_" + idEtapa);
            Files.createDirectories(directorio);
            Path destino = directorio.resolve("ARL_" + System.currentTimeMillis() + extension);
            archivo.transferTo(destino);
            rutaWeb = "/" + destino.toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo de la ARL en el servidor: " + e.getMessage(), e);
        }

        DocumentoRequisito documento = documentoRequisitoRepository
                .findByEtapaProductivaIdEtapaAndPlantillaFormatoIdPlantilla(idEtapa, plantillaArl.getIdPlantilla())
                .orElseGet(DocumentoRequisito::new);
        documento.setEtapaProductiva(etapa);
        documento.setPlantillaFormato(plantillaArl);
        documento.setRutaArchivoLleno(rutaWeb);
        documento.setEstadoValidacion(EstadoValidacion.APROBADO);
        documento.setFechaSubida(LocalDateTime.now());
        documentoRequisitoRepository.save(documento);
    }

    /**
     * Aplica el mismo criterio de búsqueda del buscador de la vista sobre todos los campos,
     * para que la exportación respete el filtro que el Gestor tiene en pantalla.
     */
    public List<AprendizGestionDto> filtrar(List<AprendizGestionDto> aprendices, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return aprendices;
        }
        String criterio = filtro.trim().toLowerCase(Locale.ROOT);
        return aprendices.stream()
                .filter(a -> String.join(" ", valores(a)).toLowerCase(Locale.ROOT).contains(criterio))
                .toList();
    }

    // 📊 Genera el libro Excel (.xlsx) con la tabla completa de aprendices
    public byte[] generarExcel(List<AprendizGestionDto> aprendices) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Aprendices");

            XSSFFont fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());

            CellStyle estiloTitulo = libro.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row filaTitulos = hoja.createRow(0);
            for (int i = 0; i < TITULOS.length; i++) {
                Cell celda = filaTitulos.createCell(i);
                celda.setCellValue(TITULOS[i]);
                celda.setCellStyle(estiloTitulo);
            }

            int numeroFila = 1;
            for (AprendizGestionDto aprendiz : aprendices) {
                Row fila = hoja.createRow(numeroFila++);
                String[] valores = valores(aprendiz);
                for (int i = 0; i < valores.length; i++) {
                    fila.createCell(i).setCellValue(valores[i]);
                }
            }

            for (int i = 0; i < TITULOS.length; i++) {
                hoja.autoSizeColumn(i);
            }

            libro.write(salida);
            return salida.toByteArray();
        }
    }

    // 📄 Genera el PDF apaisado con la tabla completa de aprendices
    public byte[] generarPdf(List<AprendizGestionDto> aprendices) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 16, 16, 22, 22);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        Paragraph titulo = new Paragraph("KRONOS - Gestión de Aprendices en Etapa Productiva",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(5, 112, 21)));
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph fechaGeneracion = new Paragraph("Generado el " + LocalDate.now().format(FORMATO_FECHA),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
        fechaGeneracion.setAlignment(Element.ALIGN_CENTER);
        fechaGeneracion.setSpacingAfter(12);
        documento.add(fechaGeneracion);

        PdfPTable tabla = new PdfPTable(TITULOS.length);
        tabla.setWidthPercentage(100);

        com.lowagie.text.Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.5f, Color.WHITE);
        for (String tituloColumna : TITULOS) {
            PdfPCell celda = new PdfPCell(new Paragraph(tituloColumna, fuenteTitulo));
            celda.setBackgroundColor(new Color(5, 112, 21));
            celda.setPadding(3);
            tabla.addCell(celda);
        }

        com.lowagie.text.Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 5.5f);
        for (AprendizGestionDto aprendiz : aprendices) {
            for (String valorCelda : valores(aprendiz)) {
                PdfPCell celda = new PdfPCell(new Paragraph(valorCelda, fuenteCelda));
                celda.setPadding(2.5f);
                tabla.addCell(celda);
            }
        }

        documento.add(tabla);
        documento.close();
        return salida.toByteArray();
    }

    // El DocumentoRequisito de la ARL radicado para la etapa (si existe)
    private DocumentoRequisito buscarDocumentoArl(Long idEtapa) {
        return documentoRequisitoRepository.findByEtapaProductivaIdEtapa(idEtapa).stream()
                .filter(d -> d.getPlantillaFormato() != null
                        && d.getPlantillaFormato().getNombreDocumento() != null
                        && d.getPlantillaFormato().getNombreDocumento().toUpperCase(Locale.ROOT).contains(NOMBRE_PLANTILLA_ARL))
                .findFirst()
                .orElse(null);
    }

    private String[] valores(AprendizGestionDto a) {
        return new String[]{
                a.getInstructorSeguimiento(),
                a.getTipoDocumento(), a.getDocumento(), a.getApellidos(), a.getNombres(),
                a.getTelefono(), a.getCorreoElectronico(), a.getNivelFormacion(),
                a.getProgramaFormacion(), a.getFicha(), a.getFechaFinFicha(),
                a.getRazonSocial(), a.getMunicipioEmpresa(), a.getDepartamentoEmpresa(),
                a.getCorreoEmpresa(), a.getTelefonoEmpresa(), a.getModalidadContrato(),
                a.getContratoInicio(), a.getContratoFin(), a.getArl(), a.getFechaRegistro(), a.getEstadoEtapaTexto()
        };
    }

    private String formatear(LocalDate fecha) {
        return fecha != null ? fecha.format(FORMATO_FECHA) : SIN_DATO;
    }

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? SIN_DATO : texto;
    }
}
