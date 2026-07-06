package com.etapa_productiva.kronos.entity;

/**
 * 🔍 Tipos de acción que registra el módulo de Auditoría del Administrador.
 * ⚠️ IMPORTANTE (Oracle): la columna AUDITORIA.ACCION nació como VARCHAR2(10) con un CHECK
 * de solo INSERT/UPDATE/DELETE. Al ampliar este enum hay que ejecutar el script
 * scripts/auditoria_ampliar_acciones.sql para ensanchar la columna y recrear el CHECK,
 * o los nuevos valores fallarán en runtime con ORA-02290 / ORA-12899.
 */
public enum AccionAuditoria {
    INSERT,
    UPDATE,
    DELETE,
    IMPORTACION,          // Cargas masivas de Excel (áreas, fichas, DIVIPOLA, usuarios, visitas)
    EXPORTACION,          // Descargas de reportes Excel/PDF
    ASIGNACION,           // Asignaciones de instructor a etapas/fichas
    SUBIDA_BITACORA,      // El aprendiz sube una bitácora
    EVALUACION_BITACORA,  // El instructor evalúa una bitácora
    NOVEDAD,              // Novedades radicadas sobre la etapa productiva
    ALERTA,               // Alertas generadas por los jobs automáticos
    RESET_PASSWORD        // Soporte de Credenciales: blanqueo/reseteo de contraseñas
}
