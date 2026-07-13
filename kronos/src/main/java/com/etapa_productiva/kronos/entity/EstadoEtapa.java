package com.etapa_productiva.kronos.entity;

public enum EstadoEtapa {
    EN_PROGRESO,
    APROBADO,
    REPROBADO,
    TERMINADO,
    EN_SUSPENSION,
    CANCELADO // <--- Nuevo estado para congelar el proceso temporalmente
}