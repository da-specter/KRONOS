package com.etapa_productiva.kronos.entity;

public enum EstadoSolicitud {
    PENDIENTE_REVISION,    // Aprendiz acaba de enviar la solicitud inicial
    FORMATOS_HABILITADOS,  // Coordinador aprobó primeros checks y activó descargas
    FORMATOS_ENVIADOS,     // Aprendiz subió sus formatos diligenciados
    APROBADO_EN_ETAPA,     // Coordinador calificó positivo y el aprendiz ya está activo
    RECHAZADO              // Por si no cumple con algún requisito inicial
}