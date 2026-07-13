package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.DatosFilaVisitaExcel;
import com.etapa_productiva.kronos.dto.FilaImportadaVisitaInfo;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.AreasFormacion;
import com.etapa_productiva.kronos.entity.AsignacionInstructorEtapa;
import com.etapa_productiva.kronos.entity.EstadoAcademico;
import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Empresa;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.InstructorSeguimiento;
import com.etapa_productiva.kronos.entity.ModalidadEtapa;
import com.etapa_productiva.kronos.entity.ModalidadVisita;
import com.etapa_productiva.kronos.entity.Municipio;
import com.etapa_productiva.kronos.entity.NivelFormacion;
import com.etapa_productiva.kronos.entity.ProgramasFormacion;
import com.etapa_productiva.kronos.entity.Rol;
import com.etapa_productiva.kronos.entity.TipoContrato;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.TipoVisita;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.UsuarioRol;
import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.AreasFormacionRepository;
import com.etapa_productiva.kronos.repository.EmpresaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.FichaRepository;
import com.etapa_productiva.kronos.repository.InstructorSeguimientoRepository;
import com.etapa_productiva.kronos.repository.MunicipioRepository;
import com.etapa_productiva.kronos.repository.NivelFormacionRepository;
import com.etapa_productiva.kronos.repository.ProgramasFormacionRepository;
import com.etapa_productiva.kronos.repository.RolRepository;
import com.etapa_productiva.kronos.repository.TipoContratoRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.repository.VisitaSeguimientoRepository;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * 📥 Procesa UNA fila del Excel de importación de "Visitas de Seguimiento", en su propia
 * transacción (REQUIRES_NEW): si algo falla a mitad de camino (por ejemplo, el municipio no
 * existe), se revierte solo lo de esta fila, sin afectar las filas ya procesadas con éxito.
 */
@Service
public class ImportacionVisitaFilaService {

    private static final DateTimeFormatter[] PATRONES_FECHA = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"), DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ofPattern("dd/MM/yy")
    };

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private FichaRepository fichaRepository;
    @Autowired private ProgramasFormacionRepository programasFormacionRepository;
    @Autowired private NivelFormacionRepository nivelFormacionRepository;
    @Autowired private AreasFormacionRepository areasFormacionRepository;
    @Autowired private MunicipioRepository municipioRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private TipoContratoRepository tipoContratoRepository;
    @Autowired private EtapaProductivaRepository etapaProductivaRepository;
    @Autowired private AprendizFichaRepository aprendizFichaRepository;
    @Autowired private InstructorSeguimientoRepository instructorSeguimientoRepository;
    @Autowired private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;
    @Autowired private VisitaSeguimientoRepository visitaSeguimientoRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private VisitaSeguimientoService visitaSeguimientoService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FilaImportadaVisitaInfo procesarFila(DatosFilaVisitaExcel datos, Long idUsuarioInstructor,
                                                 LocalDate fichaInicioGlobal, LocalDate fichaFinGlobal,
                                                 String tipoContratoRespaldo) {

        boolean usuarioCreado = false, fichaCreada = false, programaCreado = false,
                empresaCreada = false, tipoContratoCreado = false;

        // 1) Usuario aprendiz: si ya existe Y ya tiene una Etapa Productiva activa, se omite la fila entera
        Usuario usuario = usuarioRepository.findByDocumento(datos.documento().trim()).orElse(null);
        if (usuario != null && etapaProductivaRepository.findByAprendizIdUsuario(usuario.getIdUsuario()).isPresent()) {
            return FilaImportadaVisitaInfo.OMITIDA;
        }
        if (usuario == null) {
            usuario = crearAprendiz(datos);
            usuarioCreado = true;
        }

        // 2) Nivel de formación: debe existir ya en el catálogo (evita duplicar por tildes/mayúsculas)
        String nivelTexto = datos.nivelFormacion() == null ? "" : datos.nivelFormacion().trim();
        NivelFormacion nivel = nivelFormacionRepository.findByNombreNivelIgnoreCase(nivelTexto)
                .orElseThrow(() -> new IllegalArgumentException("El nivel de formación \"" + nivelTexto + "\" no existe en el catálogo de KRONOS."));

        // 3) Programa de formación: se reutiliza o se crea (igual que en Gestión de Fichas)
        boolean[] programaNuevoFlag = {false};
        ProgramasFormacion programa = obtenerOCrearPrograma(datos.programaFormacion(), nivel, programaNuevoFlag);
        programaCreado = programaNuevoFlag[0];

        // 4) Ficha: se reutiliza por número, o se crea con las fechas globales del import
        boolean[] fichaNuevaFlag = {false};
        String numeroFicha = (datos.numeroFicha() == null || datos.numeroFicha().isBlank()) ? "SIN-FICHA" : datos.numeroFicha().trim();
        Ficha ficha = fichaRepository.findByNumeroFicha(numeroFicha).orElseGet(() -> {
            fichaNuevaFlag[0] = true;
            return fichaRepository.save(Ficha.builder()
                    .numeroFicha(numeroFicha.length() > 7 ? numeroFicha.substring(0, 7) : numeroFicha)
                    .fechaInicio(fichaInicioGlobal).fechaFin(fichaFinGlobal).estado(true)
                    .programaFormacion(programa).build());
        });
        fichaCreada = fichaNuevaFlag[0];

        // 5) Matrícula (AprendizFicha), si no existe
        if (!aprendizFichaRepository.existsByUsuarioIdUsuarioAndFichaIdFicha(usuario.getIdUsuario(), ficha.getIdFicha())) {
            aprendizFichaRepository.save(AprendizFicha.builder()
                    .usuario(usuario).ficha(ficha).estadoAcademico(EstadoAcademico.INICIADO).build());
        }
        AprendizFicha matricula = aprendizFichaRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new IllegalStateException("No se pudo registrar la matrícula del aprendiz."));

        // 6) Municipio del sitio de práctica: debe existir ya en el catálogo (no se adivina el departamento)
        String municipioTexto = datos.municipio() == null ? "" : datos.municipio().trim();
        Municipio municipio = municipioRepository.findFirstByNombreMunicipioIgnoreCase(municipioTexto)
                .orElseThrow(() -> new IllegalArgumentException("El municipio \"" + municipioTexto + "\" no existe en el catálogo de KRONOS."));

        // 7) Empresa: se reutiliza por NIT, o se crea
        String nit = valor(datos.nitEmpresa());
        Empresa empresa = empresaRepository.findByNit(nit).orElse(null);
        if (empresa == null) {
            empresa = empresaRepository.save(Empresa.builder()
                    .nit(nit)
                    .nombreEmpresa(valor(datos.razonSocial()))
                    .direccion(valor(datos.direccionEmpresa()))
                    .municipio(municipio)
                    .estado(true)
                    .build());
            empresaCreada = true;
        }

        // 8) Tipo de contrato: se autodetecta el texto de la columna del Excel; si no coincide con
        //    ningún tipo existente, se usa (o crea) el respaldo indicado una sola vez para todo el lote
        boolean[] tipoContratoNuevoFlag = {false};
        TipoContrato tipoContrato = obtenerOCrearTipoContrato(datos.tipoContrato(), tipoContratoRespaldo, tipoContratoNuevoFlag);
        tipoContratoCreado = tipoContratoNuevoFlag[0];

        // 9) Fechas de la Etapa Productiva: fin = FECHA TERMINACION; inicio = fin - 6 meses
        LocalDate fechaFinEtapa = parseFecha(datos.fechaTerminacion());
        if (fechaFinEtapa == null) {
            throw new IllegalArgumentException("La fecha de terminación \"" + datos.fechaTerminacion() + "\" no es una fecha válida.");
        }
        LocalDate fechaInicioEtapa = fechaFinEtapa.minusMonths(6);

        // 10) Etapa Productiva
        EtapaProductiva etapa = etapaProductivaRepository.save(EtapaProductiva.builder()
                .aprendizFicha(matricula)
                .empresa(empresa)
                .tipoContrato(tipoContrato)
                .fechaInicio(fechaInicioEtapa)
                .fechaFin(fechaFinEtapa)
                .modalidad(ModalidadEtapa.PRESENCIAL) // el Excel no trae la modalidad de la etapa, solo la de la visita
                .nombreJefeInmediato(valor(datos.jefeNombre()))
                .correoJefeInmediato(valor(datos.jefeCorreo()))
                .telefonoJefeInmediato(valor(datos.jefeTelefono()))
                .build());

        // 11) Asignación del Instructor de Seguimiento que hizo la importación
        InstructorSeguimiento instructor = instructorSeguimientoRepository.findByUsuarioIdUsuario(idUsuarioInstructor)
                .orElseThrow(() -> new IllegalStateException("Tu perfil de Instructor de Seguimiento no está configurado."));
        Usuario usuarioInstructor = usuarioRepository.findById(idUsuarioInstructor)
                .orElseThrow(() -> new IllegalStateException("Usuario del instructor no encontrado."));
        asignacionInstructorEtapaRepository.save(AsignacionInstructorEtapa.builder()
                .etapaProductiva(etapa)
                .instructor(instructor)
                .fechaAsignacion(LocalDateTime.now())
                .estadoAsignacion(true)
                .build());

        // 12) Visita de Seguimiento
        LocalDate fechaVisita = parseFecha(datos.fechaVisita());
        if (fechaVisita == null) {
            throw new IllegalArgumentException("La fecha de visita \"" + datos.fechaVisita() + "\" no es una fecha válida.");
        }
        TipoVisita tipoVisita = visitaSeguimientoService.mapearTipoFlexible(datos.tipoVisita());
        ModalidadVisita modalidadVisita = visitaSeguimientoService.mapearModalidadFlexible(datos.modalidadVisita());
        String numeroActa = datos.numeroActa() == null || datos.numeroActa().isBlank() ? null : recortar(datos.numeroActa(), 50);

        visitaSeguimientoRepository.save(VisitaSeguimiento.builder()
                .etapaProductiva(etapa)
                .instructor(usuarioInstructor)
                .fechaVisita(fechaVisita.atStartOfDay())
                .numeroActa(numeroActa)
                .modalidad(modalidadVisita)
                .tipoVisita(tipoVisita)
                .estadoVisita(EstadoVisita.PLANEADA)
                .build());

        return new FilaImportadaVisitaInfo(usuarioCreado, fichaCreada, programaCreado, empresaCreada,
                tipoContratoCreado, true, true, false);
    }

    // ─────────────────────────────── Helpers ───────────────────────────────

    private Usuario crearAprendiz(DatosFilaVisitaExcel datos) {
        Rol rolAprendiz = rolRepository.findByNombreRol("APRENDIZ")
                .orElseThrow(() -> new IllegalStateException("No existe el rol APRENDIZ en el sistema."));

        String[] nombreApellido = dividirNombreApellido(datos.nombreCompleto());
        Usuario nuevo = Usuario.builder()
                .tipoDocumento(parseTipoDocumento(datos.tipoDocumento()))
                .documento(recortar(datos.documento().trim(), 10))
                .nombre(recortar(nombreApellido[0], 30))
                .apellido(recortar(nombreApellido[1], 30))
                .correoElectronico(datos.documento().trim() + "@aprendiz.kronos.local")
                .password(passwordEncoder.encode(datos.documento().trim()))
                .estado(true)
                .usuarioRoles(new ArrayList<>())
                .build();
        Usuario guardado = usuarioRepository.save(nuevo);
        guardado.getUsuarioRoles().add(UsuarioRol.builder().usuario(guardado).rol(rolAprendiz).build());
        return usuarioRepository.save(guardado);
    }

    private ProgramasFormacion obtenerOCrearPrograma(String nombre, NivelFormacion nivel, boolean[] creado) {
        String limpio = (nombre == null || nombre.isBlank()) ? "SIN PROGRAMA" : nombre.trim();
        return programasFormacionRepository.findFirstByNombreProgramaIgnoreCase(limpio)
                .orElseGet(() -> {
                    AreasFormacion area = areasFormacionRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("No hay áreas de formación base para crear el programa."));
                    creado[0] = true;
                    return programasFormacionRepository.save(ProgramasFormacion.builder()
                            .nombrePrograma(limpio).estado(true).areaFormacion(area).nivelFormacion(nivel).build());
                });
    }

    private TipoContrato obtenerOCrearTipoContrato(String textoColumna, String respaldo, boolean[] creado) {
        String limpio = textoColumna == null ? "" : textoColumna.trim();
        if (!limpio.isEmpty()) {
            var existente = tipoContratoRepository.findByNombreTipoContratoIgnoreCase(limpio);
            if (existente.isPresent()) return existente.get();
        }
        String textoRespaldo = (respaldo == null || respaldo.isBlank()) ? limpio : respaldo.trim();
        if (textoRespaldo.isBlank()) {
            throw new IllegalArgumentException("No se pudo determinar el tipo de contrato (columna vacía y sin valor de respaldo).");
        }
        return tipoContratoRepository.findByNombreTipoContratoIgnoreCase(textoRespaldo)
                .orElseGet(() -> {
                    creado[0] = true;
                    return tipoContratoRepository.save(TipoContrato.builder()
                            .nombreTipoContrato(recortar(textoRespaldo, 50)).estado(true).build());
                });
    }

    private String[] dividirNombreApellido(String nombreCompleto) {
        String limpio = nombreCompleto == null ? "" : nombreCompleto.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) return new String[]{"Aprendiz", "Importado"};
        String[] palabras = limpio.split(" ");
        if (palabras.length == 1) return new String[]{palabras[0], "Importado"};
        int nombreCount = palabras.length == 2 ? 1 : palabras.length - 2;
        String nombre = String.join(" ", Arrays.copyOfRange(palabras, 0, nombreCount));
        String apellido = String.join(" ", Arrays.copyOfRange(palabras, nombreCount, palabras.length));
        return new String[]{nombre, apellido};
    }

    private TipoDocumento parseTipoDocumento(String tipo) {
        if (tipo == null) return TipoDocumento.CC;
        String t = tipo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        if (t.startsWith("TI")) return TipoDocumento.TI;
        if (t.startsWith("CE")) return TipoDocumento.CE;
        if (t.startsWith("PA")) return TipoDocumento.PASAPORTE;
        return TipoDocumento.CC;
    }

    private LocalDate parseFecha(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        if (t.isEmpty()) return null;
        for (DateTimeFormatter patron : PATRONES_FECHA) {
            try {
                return LocalDate.parse(t, patron);
            } catch (Exception ignored) {
                // probar el siguiente patrón
            }
        }
        return null;
    }

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? "" : texto.trim();
    }

    private String recortar(String texto, int max) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
