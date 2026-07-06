package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.CodigoRecuperacion;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.CodigoRecuperacionRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 🔑 Módulo "Olvidé mi contraseña" del login: por ahora solo soporta el canal de
 * correo electrónico (ver EmailService). El canal de mensaje de texto queda deshabilitado
 * en la vista hasta que el proyecto integre un proveedor de SMS real.
 */
@Service
public class PasswordRecoveryService {

    private static final int MINUTOS_EXPIRACION = 15;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CodigoRecuperacionRepository codigoRecuperacionRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void solicitarCodigoPorCorreo(String correoElectronico) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico)
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún usuario registrado con ese correo."));

        String codigo = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        CodigoRecuperacion codigoRecuperacion = CodigoRecuperacion.builder()
                .usuario(usuario)
                .codigo(codigo)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION))
                .usado(false)
                .build();
        codigoRecuperacionRepository.save(codigoRecuperacion);

        String asunto = "KRONOS - Código para restablecer tu contraseña";
        String cuerpo = "Hola " + usuario.getNombre() + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña en KRONOS.\n"
                + "Tu código de verificación es: " + codigo + "\n\n"
                + "Este código vence en " + MINUTOS_EXPIRACION + " minutos. Si tú no solicitaste este cambio, ignora este mensaje.";
        emailService.enviarSiHabilitado(usuario.getCorreoElectronico(), asunto, cuerpo);
    }

    @Transactional
    public void restablecerContrasena(String correoElectronico, String codigo, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico)
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún usuario registrado con ese correo."));

        CodigoRecuperacion codigoRecuperacion = codigoRecuperacionRepository
                .findTopByUsuarioIdUsuarioAndCodigoAndUsadoFalseOrderByIdCodigoDesc(usuario.getIdUsuario(), codigo)
                .orElseThrow(() -> new IllegalArgumentException("El código ingresado no es válido."));

        if (codigoRecuperacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El código ingresado ya venció. Solicita uno nuevo.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);

        codigoRecuperacion.setUsado(true);
        codigoRecuperacionRepository.save(codigoRecuperacion);
    }
}
