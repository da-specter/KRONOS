package com.etapa_productiva.kronos.entity;

public enum EstadoEtapa {
    EN_PROGRESO,
    APROBADO,
    REPROBADO,
    POR_CERTIFICAR,
    CERTIFICADO,
    EN_SUSPENSION // <--- Nuevo estado para congelar el proceso temporalmente
}