package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.ResultadoImportacionAdmin;
import com.etapa_productiva.kronos.dto.UsuarioAdminDto;
import com.etapa_productiva.kronos.entity.*;
import com.etapa_productiva.kronos.repository.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 👥 Control de Accesos del Administrador: registro de usuarios, asignación de roles,
 * activación/desactivación de cuentas, carga masiva por rol y soporte de credenciales
 * (blanqueo/reseteo de contraseñas). Todo deja rastro en AUDITORIA.
 *
 * Contraseña inicial: el número de documento (encriptado con BCrypt). Al blanquear una
 * cuenta, la contraseña vuelve a ser el documento.
 */
@Service
public class AdminUsuariosService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private FichaRepository fichaRepository;
    @Autowired private AprendizFichaRepository aprendizFichaRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditoriaService auditoriaService;

    // ══════════════════════════ Listado ══════════════════════════

    @Transactional(readOnly = true)
    public List<UsuarioAdminDto> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(u -> new UsuarioAdminDto(
                        u.getIdUsuario(),
                        u.getTipoDocumento() == null ? "" : u.getTipoDocumento().name(),
                        u.getDocumento(),
                        u.getNombre(),
                        u.getApellido(),
                        u.getCorreoElectronico(),
                        u.getTelefono(),
                        u.getEstado(),
                        u.getUsuarioRoles() == null ? List.of()
                                : u.getUsuarioRoles().stream().map(ur -> ur.getRol().getNombreRol()).toList()))
                .toList();
    }

    // ══════════════════════════ Registro y roles ══════════════════════════

    @Transactional
    public Usuario crearUsuario(TipoDocumento tipoDocumento, String documento, String nombre, String apellido,
                                String correo, String telefono, String nombreRol, Long idAdmin) {
        if (documento == null || documento.isBlank() || nombre == null || nombre.isBlank()
                || apellido == null || apellido.isBlank() || correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("Documento, nombre, apellido y correo son obligatorios.");
        }
        if (usuarioRepository.findByDocumento(documento.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con el documento " + documento.trim() + ".");
        }
        if (usuarioRepository.findByCorreoElectronico(correo.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo " + correo.trim() + ".");
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .tipoDocumento(tipoDocumento == null ? TipoDocumento.CC : tipoDocumento)
                .documento(documento.trim())
                .nombre(nombre.trim())
                .apellido(apellido.trim())
                .correoElectronico(correo.trim())
                .telefono(telefono == null || telefono.isBlank() ? null : telefono.trim())
                .password(passwordEncoder.encode(documento.trim())) // contraseña inicial = documento
                .build());

        asignarRol(usuario.getIdUsuario(), nombreRol, idAdmin, false);

        auditoriaService.registrar(idAdmin, AccionAuditoria.INSERT,
                "Usuario creado: " + usuario.getNombre() + " " + usuario.getApellido()
                        + " (" + usuario.getDocumento() + ") con rol " + nombreRol);
        return usuario;
    }

    @Transactional
    public void asignarRol(Long idUsuario, String nombreRol, Long idAdmin, boolean auditar) {
        if (nombreRol == null || nombreRol.isBlank()) {
            throw new IllegalArgumentException("Debes elegir el rol a asignar.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        Rol rol = rolRepository.findByNombreRol(nombreRol.trim())
                .orElseThrow(() -> new IllegalArgumentException("El rol '" + nombreRol + "' no existe en el sistema."));

        if (usuarioRolRepository.existsByUsuarioIdUsuarioAndRolIdRol(idUsuario, rol.getIdRol())) {
            throw new IllegalArgumentException("El usuario ya tiene el rol " + nombreRol + ".");
        }

        usuarioRolRepository.save(UsuarioRol.builder().usuario(usuario).rol(rol).build());

        if (auditar) {
            auditoriaService.registrar(idAdmin, AccionAuditoria.UPDATE,
                    "Rol " + nombreRol + " asignado a " + usuario.getNombre() + " " + usuario.getApellido()
                            + " (" + usuario.getDocumento() + ")");
        }
    }

    @Transactional
    public void quitarRol(Long idUsuario, String nombreRol, Long idAdmin) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));

        List<UsuarioRol> asignaciones = usuarioRolRepository.findByUsuarioIdUsuario(idUsuario);
        UsuarioRol objetivo = asignaciones.stream()
                .filter(ur -> ur.getRol().getNombreRol().equalsIgnoreCase(nombreRol))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene el rol " + nombreRol + "."));

        if (asignaciones.size() == 1) {
            throw new IllegalArgumentException("No puedes dejar al usuario sin ningún rol.");
        }

        usuarioRolRepository.delete(objetivo);
        auditoriaService.registrar(idAdmin, AccionAuditoria.DELETE,
                "Rol " + nombreRol + " retirado a " + usuario.getNombre() + " " + usuario.getApellido()
                        + " (" + usuario.getDocumento() + ")");
    }

    @Transactional
    public Usuario cambiarEstado(Long idUsuario, boolean activar, Long idAdmin) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        usuario.setEstado(activar);
        usuario = usuarioRepository.save(usuario);
        auditoriaService.registrar(idAdmin, AccionAuditoria.UPDATE,
                "Cuenta " + (activar ? "activada" : "desactivada") + ": " + usuario.getNombre() + " "
                        + usuario.getApellido() + " (" + usuario.getDocumento() + ")");
        return usuario;
    }

    // ══════════════════════════ Carga masiva por rol ══════════════════════════

    /**
     * 📥 Carga masiva de usuarios (aprendices, instructores técnicos, instructores de
     * seguimiento o gestores según el rol elegido en el formulario). Columnas del Excel:
     * "DOCUMENTO" y "NOMBRE" obligatorias; "TP"/"TIPO DOC", "APELLIDO", "CORREO" y
     * "TELEFONO" opcionales. Para rol APRENDIZ, la columna "FICHA" (opcional) matricula
     * al aprendiz en esa ficha si ya existe en el catálogo.
     */
    @Transactional
    public ResultadoImportacionAdmin importarUsuarios(MultipartFile archivo, String nombreRol, Long idAdmin) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar el archivo Excel a importar.");
        }
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!nombreArchivo.endsWith(".xlsx") && !nombreArchivo.endsWith(".xls")) {
            throw new IllegalArgumentException("El archivo a importar debe ser un Excel (.xlsx o .xls).");
        }
        Rol rol = rolRepository.findByNombreRol(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException("El rol '" + nombreRol + "' no existe en el sistema."));

        int filas = 0, creados = 0, actualizados = 0, omitidos = 0;
        List<String> errores = new ArrayList<>();

        try (InputStream in = archivo.getInputStream(); Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            int filaCabecera = ubicarCabecera(hoja, fmt);
            if (filaCabecera < 0) {
                throw new IllegalArgumentException("No se encontraron las columnas 'DOCUMENTO' y 'NOMBRE' en el Excel.");
            }
            Row cab = hoja.getRow(filaCabecera);
            int colTipoDoc = colTipoDocumento(cab, fmt);
            int colDocumento = col(cab, fmt, "no. doc", "no doc", "documento", "cedula", "cédula");
            int colNombre = col(cab, fmt, "nombre");
            int colApellido = col(cab, fmt, "apellido");
            int colCorreo = col(cab, fmt, "correo", "email");
            int colTelefono = col(cab, fmt, "telefono", "teléfono", "celular");
            int colFicha = col(cab, fmt, "ficha");

            for (int i = filaCabecera + 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                if (row == null) continue;
                String documento = leer(row, colDocumento, fmt);
                if (documento.isBlank() || documento.chars().noneMatch(Character::isDigit)) continue;
                filas++;

                try {
                    String nombreCompleto = leer(row, colNombre, fmt);
                    String apellido = leer(row, colApellido, fmt);
                    if (apellido.isBlank()) {
                        // Sin columna APELLIDO: se divide el nombre completo por la mitad
                        String[] partes = nombreCompleto.trim().split("\\s+");
                        if (partes.length >= 2) {
                            int mitad = partes.length / 2;
                            nombreCompleto = String.join(" ", java.util.Arrays.copyOfRange(partes, 0, mitad));
                            apellido = String.join(" ", java.util.Arrays.copyOfRange(partes, mitad, partes.length));
                        } else {
                            apellido = "-";
                        }
                    }
                    if (nombreCompleto.isBlank()) {
                        errores.add("Fila " + (i + 1) + ": el documento " + documento + " no trae nombre.");
                        continue;
                    }

                    String correo = leer(row, colCorreo, fmt);
                    if (correo.isBlank()) {
                        // Correo institucional provisional para no violar el NOT NULL/UNIQUE de Oracle
                        correo = documento.trim() + "@pendiente.kronos.co";
                    }

                    Usuario usuario = usuarioRepository.findByDocumento(documento.trim()).orElse(null);
                    boolean usuarioNuevo = usuario == null;

                    if (usuarioNuevo) {
                        if (usuarioRepository.findByCorreoElectronico(correo.trim()).isPresent()) {
                            errores.add("Fila " + (i + 1) + ": el correo " + correo + " ya pertenece a otro usuario.");
                            continue;
                        }
                        usuario = usuarioRepository.save(Usuario.builder()
                                .tipoDocumento(parsearTipoDocumento(leer(row, colTipoDoc, fmt)))
                                .documento(documento.trim())
                                .nombre(recortar(nombreCompleto, 30))
                                .apellido(recortar(apellido, 30))
                                .correoElectronico(correo.trim())
                                .telefono(leer(row, colTelefono, fmt).isBlank() ? null : recortar(leer(row, colTelefono, fmt), 11))
                                .password(passwordEncoder.encode(documento.trim()))
                                .build());
                        creados++;
                    }

                    // Rol elegido en el formulario (si ya lo tiene, no se duplica)
                    if (!usuarioRolRepository.existsByUsuarioIdUsuarioAndRolIdRol(usuario.getIdUsuario(), rol.getIdRol())) {
                        usuarioRolRepository.save(UsuarioRol.builder().usuario(usuario).rol(rol).build());
                        if (!usuarioNuevo) actualizados++;
                    } else if (!usuarioNuevo) {
                        omitidos++;
                    }

                    // Solo para APRENDIZ: matrícula en la ficha si el Excel trae la columna FICHA
                    if ("APRENDIZ".equalsIgnoreCase(nombreRol)) {
                        String numeroFicha = leer(row, colFicha, fmt);
                        if (!numeroFicha.isBlank()) {
                            Ficha ficha = fichaRepository.findByNumeroFicha(numeroFicha.trim()).orElse(null);
                            if (ficha == null) {
                                errores.add("Fila " + (i + 1) + ": la ficha " + numeroFicha + " no existe (el usuario sí quedó creado).");
                            } else if (!aprendizFichaRepository.existsByUsuarioIdUsuarioAndFichaIdFicha(usuario.getIdUsuario(), ficha.getIdFicha())
                                    && aprendizFichaRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).isEmpty()) {
                                aprendizFichaRepository.save(AprendizFicha.builder()
                                        .usuario(usuario)
                                        .ficha(ficha)
                                        .build());
                            }
                        }
                    }
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo Excel: " + e.getMessage(), e);
        }

        ResultadoImportacionAdmin resultado = new ResultadoImportacionAdmin(filas, creados, actualizados, omitidos, errores);
        auditoriaService.registrar(idAdmin, AccionAuditoria.IMPORTACION,
                "Carga masiva de usuarios con rol " + nombreRol + " (" + archivo.getOriginalFilename() + "): " + resultado.resumen());
        return resultado;
    }

    // ══════════════════════════ Soporte de Credenciales ══════════════════════════

    /**
     * 🔑 Blanquea o resetea la contraseña de un usuario bloqueado. Si no se indica una
     * contraseña nueva, la cuenta queda "blanqueada": la contraseña vuelve a ser el documento.
     * Opcionalmente reactiva la cuenta en el mismo paso.
     */
    @Transactional
    public Usuario resetearContrasena(String documentoOCorreo, String nuevaContrasena, boolean reactivarCuenta, Long idAdmin) {
        if (documentoOCorreo == null || documentoOCorreo.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el documento o correo del usuario.");
        }
        String criterio = documentoOCorreo.trim();
        Usuario usuario = usuarioRepository.findByDocumento(criterio)
                .or(() -> usuarioRepository.findByCorreoElectronico(criterio))
                .orElseThrow(() -> new IllegalArgumentException("No existe un usuario con documento o correo '" + criterio + "'."));

        boolean blanqueo = nuevaContrasena == null || nuevaContrasena.isBlank();
        usuario.setPassword(passwordEncoder.encode(blanqueo ? usuario.getDocumento() : nuevaContrasena.trim()));
        if (reactivarCuenta) {
            usuario.setEstado(true);
        }
        usuario = usuarioRepository.save(usuario);

        auditoriaService.registrar(idAdmin, AccionAuditoria.RESET_PASSWORD,
                (blanqueo ? "Contraseña blanqueada (vuelve a ser el documento)" : "Contraseña reseteada")
                        + " para " + usuario.getNombre() + " " + usuario.getApellido()
                        + " (" + usuario.getDocumento() + ")" + (reactivarCuenta ? " y cuenta reactivada" : ""));
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorDocumentoOCorreo(String criterio) {
        if (criterio == null || criterio.isBlank()) return null;
        return usuarioRepository.findByDocumento(criterio.trim())
                .or(() -> usuarioRepository.findByCorreoElectronico(criterio.trim()))
                .orElse(null);
    }

    // ══════════════════════════ Helpers de lectura del Excel ══════════════════════════

    private TipoDocumento parsearTipoDocumento(String texto) {
        if (texto == null || texto.isBlank()) return TipoDocumento.CC;
        String limpio = texto.trim().toUpperCase(Locale.ROOT);
        for (TipoDocumento tipo : TipoDocumento.values()) {
            if (tipo.name().equals(limpio)) return tipo;
        }
        if (limpio.startsWith("PAS")) return TipoDocumento.PASAPORTE;
        return TipoDocumento.CC;
    }

    private String recortar(String texto, int maximo) {
        if (texto == null) return null;
        String limpio = texto.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    private int ubicarCabecera(Sheet hoja, DataFormatter fmt) {
        int limite = Math.min(hoja.getLastRowNum(), 8);
        for (int i = hoja.getFirstRowNum(); i <= limite; i++) {
            Row row = hoja.getRow(i);
            if (row == null) continue;
            boolean tieneDoc = col(row, fmt, "no. doc", "no doc", "documento", "cedula", "cédula") >= 0;
            boolean tieneNombre = col(row, fmt, "nombre") >= 0;
            if (tieneDoc && tieneNombre) return i;
        }
        return -1;
    }

    // "TP" es un encabezado muy corto: exige coincidencia exacta o "tipo doc"
    private int colTipoDocumento(Row cabecera, DataFormatter fmt) {
        for (Cell celda : cabecera) {
            String texto = fmt.formatCellValue(celda).toLowerCase(Locale.ROOT).trim();
            if (texto.equals("tp") || (texto.contains("tipo") && texto.contains("doc"))) {
                return celda.getColumnIndex();
            }
        }
        return -1;
    }

    private int col(Row cabecera, DataFormatter fmt, String... alias) {
        if (cabecera == null) return -1;
        for (Cell celda : cabecera) {
            String texto = fmt.formatCellValue(celda).toLowerCase(Locale.ROOT).trim();
            for (String a : alias) {
                if (texto.contains(a.toLowerCase(Locale.ROOT))) return celda.getColumnIndex();
            }
        }
        return -1;
    }

    private String leer(Row row, int col, DataFormatter fmt) {
        if (col < 0) return "";
        Cell celda = row.getCell(col);
        return celda == null ? "" : fmt.formatCellValue(celda).trim();
    }
}
