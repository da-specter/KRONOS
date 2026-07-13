package com.etapa_productiva.kronos.entity;

public enum EstadoSolicitud {
    PENDIENTE_REVISION,      // Aprendiz acaba de enviar la solicitud inicial
    FORMATOS_HABILITADOS,    // Coordinador aprobó primeros checks y activó descargas
    FORMATOS_ENVIADOS,       // Aprendiz subió sus formatos diligenciados
    EN_VALIDACION_REGISTRO,  // Gestor de Etapa calificó los documentos y los envió al rol Registro
    LISTO_PARA_REGISTRO,     // Registro validó los documentos: ya puede registrar la Etapa Productiva
    APROBADO_EN_ETAPA,       // Registro creó la Etapa Productiva y el aprendiz ya está activo
    RECHAZADO,               // Por si no cumple con algún requisito del flujo
    PENDIENTE_REGISTRO // Contrato de Aprendizaje: va directo a Registro, sin pasar por el Gestor de Etapa
}