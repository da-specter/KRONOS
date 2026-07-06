package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.AccionAuditoria;
import com.etapa_productiva.kronos.entity.Auditoria;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.AuditoriaRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 🔍 Punto único para dejar rastro en la tabla AUDITORIA (módulo de Auditoría del Administrador).
 * Registra inserts, updates, deletes, importaciones, exportaciones, asignaciones, bitácoras,
 * novedades y alertas de todos los roles. Nunca lanza excepción: si la auditoría falla,
 * el flujo de negocio que la invocó no se ve afectado.
 */
@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public void registrar(Long idUsuario, AccionAuditoria accion, String descripcion) {
        try {
            Usuario usuario = idUsuario == null ? null : usuarioRepository.findById(idUsuario).orElse(null);

            // La columna DESCRIPCION es VARCHAR2(250): se recorta para no reventar el INSERT
            String texto = descripcion == null ? "" : descripcion;
            if (texto.length() > 250) {
                texto = texto.substring(0, 247) + "...";
            }

            auditoriaRepository.save(Auditoria.builder()
                    .usuario(usuario)
                    .accion(accion)
                    .descripcion(texto)
                    .build());
        } catch (Exception e) {
            System.out.println("⚠️ [AUDITORIA] No se pudo registrar el evento (" + accion + "): " + e.getMessage());
        }
    }
}
