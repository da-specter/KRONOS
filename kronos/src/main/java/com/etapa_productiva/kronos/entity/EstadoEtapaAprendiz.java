package com.etapa_productiva.kronos.entity;

/**
 * 🚦 Clasificación semáforo del aprendiz en el módulo "Gestión Aprendices" del Gestor de
 * Etapa, según el formato oficial del SENA: dónde está parado en la línea de tiempo de su
 * Etapa Práctica (antes de iniciarla, durante, o después de terminarla).
 */
public enum EstadoEtapaAprendiz {
    TERMINO_CONTRATO,      // 🔴 Ya pasó la fecha de fin de su Etapa Productiva
    EP_POR_TERMINAR,       // 🟠 En Etapa Práctica, a 1 mes o menos de terminar el contrato
    EN_EP,                 // 🟡 En Etapa Práctica, en progreso normal
    INICIA_EP_PRONTO,      // 🟢 Aún no inicia, a 1 mes o menos de habilitarse su Etapa Práctica
    FALTA_MAS_DE_UN_MES    // 🟩 Aún no inicia, falta más de 1 mes para habilitarse
}
