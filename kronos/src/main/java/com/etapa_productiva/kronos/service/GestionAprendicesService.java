package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.AprendizGestionDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.DocumentoRequisito;
import com.etapa_productiva.kronos.entity.Empresa;
import com.etapa_productiva.kronos.entity.EstadoValidacion;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.PlantillaFormato;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.VisibilidadDocumento;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.DocumentoRequisitoRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.PlantillaFormatoRepository;

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
    private static final String SIN_DATO = "—";
    private static final String NOMBRE_PLANTILLA_ARL = "ARL";
    private static final Set<String> EXTENSIONES_ARL = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".doc", ".docx");

    // Mismo orden de la tabla: 1º instructor, 2º aprendiz y su programa, 3º empresa y modalidad
    private static final String[] TITULOS = {
            "Instructor Seguimiento",
            "Tipo Documento", "Documento", "Apellidos", "Nombres", "Teléfono", "Correo Electrónico",
            "Nivel Formación", "Programa Formación", "Ficha", "Fecha Fin Ficha",
            "Razón Social", "Municipio Empresa", "Departamento Empresa", "Correo Empresa",
            "Teléfono Empresa", "Modalidad Contrato", "Contrato Inicio", "Contrato Fin", "ARL"
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

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    @Transactional(readOnly = true)
    public List<AprendizGestionDto> listarAprendices() {
        List<AprendizGestionDto> filas = new ArrayList<>();

        for (AprendizFicha matricula : aprendizFichaRepository.findAll()) {
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
            }

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
                    .idEtapa(etapa != null ? etapa.getIdEtapa() : null)
                    .build());
        }

        filas.sort(Comparator.comparing(AprendizGestionDto::getApellidos, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AprendizGestionDto::getNombres, String.CASE_INSENSITIVE_ORDER));
        return filas;
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
                a.getContratoInicio(), a.getContratoFin(), a.getArl()
        };
    }

    private String formatear(LocalDate fecha) {
        return fecha != null ? fecha.format(FORMATO_FECHA) : SIN_DATO;
    }

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? SIN_DATO : texto;
    }
}
