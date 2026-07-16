package com.etapa_productiva.kronos.entity;

public enum TipoNovedad {
    SUSPENSION,
    REINCORPORACION,
   APLAZAMIENTO,
    DESERCION, CAMBIO_ALT, RECLAMO, 
    OTRO,
    CAMBIO_EMPRESA,
    SOLICITUD_ETAPA,
    INFORMATIVO,
    // Chat GESTOR_ETAPA <-> COORDINADOR_ACADEMICO, mismo patrón que INFORMATIVO pero en canal aparte
    COORD_ACADEMICO,
    // Imagen de evidencia de Sofía Plus que Registro adjunta al registrar una Etapa Productiva
    EVIDENCIA_SOFIA_PLUS

}