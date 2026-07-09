package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.AccionAuditoria;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.Rol;
import com.etapa_productiva.kronos.entity.TipoDocumento;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.entity.UsuarioRol;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.FichaRepository;
import com.etapa_productiva.kronos.repository.RolRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.repository.UsuarioRolRepository;
import com.etapa_productiva.kronos.util.ValidacionCampos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🆕 Autorregistro público desde /auth/registro: cubre a los aprendices o instructores que
 * el Gestor/Admin no alcanzó a importar previamente.
 *
 * El Aprendiz se valida contra el catálogo de Fichas ya existente en KRONOS (si el número de
 * ficha no existe, no hay forma de confirmar a qué programa pertenece), así que su cuenta
 * queda ACTIVA de inmediato, igual que si lo hubiera creado el Gestor.
 *
 * El Instructor no tiene un catálogo equivalente contra el cual autovalidarse (su vínculo con
 * fichas o áreas de formación lo asigna el Gestor/Admin después, no lo puede declarar él mismo),
 * así que su cuenta nace INACTIVA y sin ningún rol: un Administrador la revisa manualmente desde
 * "Usuarios del Sistema" y decide qué tipo de instructor es y qué le asigna antes de activarla.
 */
@Service
public class RegistroPublicoService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private FichaRepository fichaRepository;
    @Autowired private AprendizFichaRepository aprendizFichaRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditoriaService auditoriaService;

    @Transactional
    public Usuario registrarAprendiz(TipoDocumento tipoDocumento, String documento, String nombre,
                                      String apellido, String correo, String telefono,
                                      String numeroFicha, String contrasena, String confirmarContrasena) {

        if (numeroFicha == null || numeroFicha.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el número de tu ficha.");
        }
        Ficha ficha = fichaRepository.findByNumeroFicha(numeroFicha.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La ficha " + numeroFicha.trim() + " no existe en KRONOS. Verifica el número con tu Instructor Técnico."));
        if (ficha.getEstado() != null && !ficha.getEstado()) {
            throw new IllegalArgumentException("La ficha " + numeroFicha.trim() + " ya no está activa en KRONOS.");
        }

        // Cuenta activa de inmediato: la existencia de la ficha ya es su validación.
        Usuario usuario = crearUsuarioBase(tipoDocumento, documento, nombre, apellido, correo, telefono,
                contrasena, confirmarContrasena, true);

        Rol rolAprendiz = rolRepository.findByNombreRol("APRENDIZ")
                .orElseThrow(() -> new IllegalStateException("No existe el rol APRENDIZ en el sistema."));
        usuarioRolRepository.save(UsuarioRol.builder().usuario(usuario).rol(rolAprendiz).build());

        aprendizFichaRepository.save(AprendizFicha.builder().usuario(usuario).ficha(ficha).build());

        auditoriaService.registrar(usuario.getIdUsuario(), AccionAuditoria.INSERT,
                "Autorregistro de aprendiz: " + usuario.getNombre() + " " + usuario.getApellido()
                        + " (" + usuario.getDocumento() + "), ficha " + ficha.getNumeroFicha());
        return usuario;
    }

    @Transactional
    public Usuario registrarSolicitudInstructor(TipoDocumento tipoDocumento, String documento, String nombre,
                                                 String apellido, String correo, String telefono,
                                                 String contrasena, String confirmarContrasena) {

        // Cuenta INACTIVA y sin rol: queda "pendiente de aprobación" hasta que un Administrador
        // la revise (ver AdminUsuariosService.aprobarSolicitud/rechazarSolicitud).
        Usuario usuario = crearUsuarioBase(tipoDocumento, documento, nombre, apellido, correo, telefono,
                contrasena, confirmarContrasena, false);

        // Actor = null: todavía no es un usuario autorizado actuando en el sistema, es una
        // solicitud anónima. Así, si el Administrador la rechaza y se borra el registro, no
        // queda ninguna fila de AUDITORIA apuntándolo (evita ORA-02292 al eliminarlo).
        auditoriaService.registrar(null, AccionAuditoria.INSERT,
                "Solicitud de acceso de instructor pendiente de aprobación: " + usuario.getNombre() + " "
                        + usuario.getApellido() + " (" + usuario.getDocumento() + ")");
        return usuario;
    }

    // Campos y validaciones comunes a ambos flujos; `activarDeInmediato` decide si la cuenta
    // nace ACTIVA (aprendiz, ya validado por ficha) o INACTIVA a la espera de un Administrador.
    private Usuario crearUsuarioBase(TipoDocumento tipoDocumento, String documento, String nombre,
                                      String apellido, String correo, String telefono,
                                      String contrasena, String confirmarContrasena, boolean activarDeInmediato) {

        if (documento == null || documento.isBlank() || nombre == null || nombre.isBlank()
                || apellido == null || apellido.isBlank() || correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("Documento, nombre, apellido y correo son obligatorios.");
        }
        ValidacionCampos.validarDocumento(documento);
        ValidacionCampos.validarNombre(nombre, "El nombre");
        ValidacionCampos.validarNombre(apellido, "El apellido");
        ValidacionCampos.validarCorreo(correo, "El correo");
        ValidacionCampos.validarTelefono(telefono, "El teléfono");

        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("Debes definir una contraseña.");
        }
        if (!contrasena.equals(confirmarContrasena)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        ValidacionCampos.validarContrasena(contrasena);

        if (usuarioRepository.findByDocumento(documento.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el documento " + documento.trim()
                    + ". Si es tuya, inicia sesión o recupera tu contraseña.");
        }
        if (usuarioRepository.findByCorreoElectronico(correo.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el correo " + correo.trim()
                    + ". Si es tuya, inicia sesión o recupera tu contraseña.");
        }

        return usuarioRepository.save(Usuario.builder()
                .tipoDocumento(tipoDocumento == null ? TipoDocumento.CC : tipoDocumento)
                .documento(documento.trim())
                .nombre(nombre.trim())
                .apellido(apellido.trim())
                .correoElectronico(correo.trim())
                .telefono(telefono == null || telefono.isBlank() ? null : telefono.trim())
                .password(passwordEncoder.encode(contrasena))
                .debeCambiarContrasena(false) // ya la eligió él mismo durante el registro
                .estado(activarDeInmediato)
                .build());
    }
}
