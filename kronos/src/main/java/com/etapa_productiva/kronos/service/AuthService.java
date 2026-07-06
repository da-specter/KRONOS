package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.dto.LoginRequest;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.MenuDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConfiguracionGlobalService configuracionGlobalService;

    @Transactional(readOnly = true)
    public LoginResponse iniciarSesion(LoginRequest request) {
        
        System.out.println("🕵️‍♂️ [SERVICE] Procesando login para: " + request.getCorreoElectronico());

        // 1. Buscar en Oracle
        Usuario usuario = usuarioRepository.findByCorreoElectronico(request.getCorreoElectronico())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        // 2. Validar contraseña con soporte para BCrypt y texto plano legacy
        if (!verificarContrasena(usuario, request.getContrasena())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        // 3. Validar estado activo
        if (usuario.getEstado() != null && !usuario.getEstado()) {
            throw new BadCredentialsException("El usuario se encuentra inactivo en el sistema.");
        }

        // 4. Mapear roles desde la base de datos
        List<String> roles = usuario.getUsuarioRoles().stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();

        System.out.println("🕵️‍♂️ [SERVICE] Roles detectados en Oracle: " + roles);

        // 4.5 🚧 Modo mantenimiento (Configuración Global): solo los Administradores pueden entrar
        if (!roles.contains("ADMINISTRADOR")
                && configuracionGlobalService.getBooleano(ConfiguracionGlobalService.MODO_MANTENIMIENTO, false)) {
            throw new BadCredentialsException(configuracionGlobalService.getValor(
                    ConfiguracionGlobalService.MENSAJE_MANTENIMIENTO,
                    "El portal KRONOS está en mantenimiento. Intenta más tarde."));
        }

        // 5. Construcción del menú dinámico según tu nuevo script de roles
        List<MenuDto> menuNavegacion = new ArrayList<>();
        
        if (roles.contains("INSTRUCTOR_SEGUIMIENTO")) {
            menuNavegacion.add(new MenuDto("Mis Aprendices", "/instructor/seguimiento"));
            // Módulo desplegable "Visitas de Seguimiento": separa el agendamiento (+ aprendices
            // pendientes por agendar) de la agenda consolidada (pasadas/pendientes/futuras).
            menuNavegacion.add(new MenuDto("Visitas de Seguimiento", null, List.of(
                    new MenuDto("Agendar Citas", "/instructor/visitas/agendar"),
                    new MenuDto("Mi agenda de visitas", "/instructor/visitas")
            )));
            // Módulo desplegable "Evaluación de Formatos": calificar Bitácoras y Formato de Planeación (023)
            // de los aprendices que tiene asignados
            menuNavegacion.add(new MenuDto("Evaluación de Formatos", null, List.of(
                    new MenuDto("Bitácoras", "/instructor/seguimiento/bitacoras"),
                    new MenuDto("Formato Planeación 023", "/instructor/seguimiento/planeacion")
            )));
            menuNavegacion.add(new MenuDto("📢 Novedades", "/novedades"));
        }
        if (roles.contains("INSTRUCTOR_TECNICO")) {
            // Módulo del líder de ficha: consulta de sus fichas/aprendices + añadir/importar aprendices
            menuNavegacion.add(new MenuDto("Mis Fichas", "/instructor/tecnico"));
        }
        if (roles.contains("APRENDIZ")) {
            menuNavegacion.add(new MenuDto("Mi Cronograma", "/aprendiz/cronograma"));
            menuNavegacion.add(new MenuDto("Mis Visitas de Seguimiento", "/aprendiz/visitas"));
            menuNavegacion.add(new MenuDto("Subir Bitácoras", "/aprendiz/bitacoras"));
            // "📁 Formatos" NO se agrega aquí: para el Aprendiz aparece de forma reactiva
            // solo cuando el Gestor de Etapa habilita su solicitud (ver IndexController/FormatosController).
        }
        if (roles.contains("GESTOR_ETAPA")) {
            // Módulo desplegable "Gestión Etapa": agrupa la consulta de fichas y de aprendices
            // para que el sidebar del Gestor no se sature de ítems sueltos.
            menuNavegacion.add(new MenuDto("Gestión Etapa", null, List.of(
                    new MenuDto("Gestión Fichas", "/coordinador/fichas"),
                    new MenuDto("Gestión Aprendices", "/gestor/aprendices")
            )));
            menuNavegacion.add(new MenuDto("Asignar Instructores", "/coordinador/asignaciones"));
            menuNavegacion.add(new MenuDto("Validación de Documentos", "/gestor/documentos"));
            menuNavegacion.add(new MenuDto("🏢 Registro Etapa Productiva", "/gestor/registro-etapa"));
            // Bandeja de aprendices que ya completaron bitácoras + Formato 023 (estado POR_CERTIFICAR)
            menuNavegacion.add(new MenuDto("🎓 Certificación Aprendiz", "/gestor/certificacion"));
            menuNavegacion.add(new MenuDto("📢 Novedades", "/novedades"));
            menuNavegacion.add(new MenuDto("📁 Formatos", "/formatos"));
        }
        if (roles.contains("ADMINISTRADOR")) {
            // 🛠️ Sidebar exclusivo del Administrador: 4 módulos madre desplegables.
            // El "Panel de Control" es el ítem fijo "Inicio" (/index), que para este rol
            // pinta la vista general del sistema (aprendices, fichas, instructores, servidores).
            menuNavegacion.add(new MenuDto("Datos Maestros", null, List.of(
                    new MenuDto("Áreas de Formación", "/admin/areas"),
                    new MenuDto("Programas de Formación", "/admin/programas"),
                    new MenuDto("Gestión de Fichas", "/admin/fichas"),
                    new MenuDto("División Territorial", "/admin/divipola")
            )));
            menuNavegacion.add(new MenuDto("Control de Accesos", null, List.of(
                    new MenuDto("Usuarios del Sistema", "/admin/usuarios"),
                    new MenuDto("Soporte de Credenciales", "/admin/credenciales")
            )));
            menuNavegacion.add(new MenuDto("Gestión de Formatos", null, List.of(
                    new MenuDto("Plantillas Oficiales", "/admin/plantillas")
            )));
            menuNavegacion.add(new MenuDto("Sistema y Automatización", null, List.of(
                    new MenuDto("Monitoreo de Jobs", "/admin/jobs"),
                    new MenuDto("Auditoría", "/admin/auditoria"),
                    new MenuDto("Configuración Global", "/admin/config")
            )));
        }

        // 👤 Disponible para cualquier rol: ver y editar los datos propios guardados en Usuario
        menuNavegacion.add(new MenuDto("👤 Mi Perfil", "/perfil"));

        return LoginResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .correo(usuario.getCorreoElectronico())
                .fotoPerfil(usuario.getFotoPerfil())
                .roles(roles)
                .menuNavegacion(menuNavegacion)
                .build();
    }

    /**
     * 🔐 Compara una contraseña en texto plano contra la guardada en Oracle,
     * soportando hashes BCrypt y contraseñas legacy en texto plano.
     * Se reutiliza en el login y en la re-autenticación previa a exportar datos.
     */
    public boolean verificarContrasena(Usuario usuario, String contrasenaIngresada) {
        if (usuario == null || contrasenaIngresada == null) {
            return false;
        }
        String passwordGuardada = usuario.getPassword();
        if (passwordGuardada != null && (passwordGuardada.startsWith("$2a$") || passwordGuardada.startsWith("$2b$") || passwordGuardada.startsWith("$2y$"))) {
            return passwordEncoder.matches(contrasenaIngresada, passwordGuardada);
        }
        return contrasenaIngresada.equals(passwordGuardada);
    }
}