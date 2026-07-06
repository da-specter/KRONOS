package com.etapa_productiva.kronos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * ✉️ Envío de correo para las alertas de KRONOS (ej. recordatorio de visita de seguimiento
 * al aprendiz). El bean {@link JavaMailSender} solo existe si se configuran credenciales SMTP
 * reales (spring.mail.host/username/password); mientras tanto este servicio queda inactivo
 * y solo deja constancia en el log, sin romper el flujo de la aplicación.
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    // 📊 Dashboard del Administrador: refleja si el servidor de correo SMTP está configurado
    public boolean estaHabilitado() {
        return mailSender != null;
    }

    public boolean enviarSiHabilitado(String destino, String asunto, String cuerpo) {
        if (mailSender == null) {
            System.out.println("✉️ [EMAIL] Envío deshabilitado (sin credenciales SMTP configuradas). Destino: " + destino + " | Asunto: " + asunto);
            return false;
        }
        if (destino == null || destino.isBlank()) {
            return false;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destino);
            if (remitente != null && !remitente.isBlank()) {
                mensaje.setFrom(remitente);
            }
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            return true;
        } catch (MailException e) {
            System.out.println("⚠️ [EMAIL] No se pudo enviar el correo a " + destino + ": " + e.getMessage());
            return false;
        }
    }
}
