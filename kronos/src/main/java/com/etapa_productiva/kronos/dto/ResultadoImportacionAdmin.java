package com.etapa_productiva.kronos.dto;

import java.util.List;

/**
 * 📥 Resumen de una carga masiva de los módulos del Administrador
 * (áreas, fichas, DIVIPOLA, usuarios): cuántas filas se procesaron,
 * cuántos registros se crearon/actualizaron/omitieron y los errores por fila.
 */
public record ResultadoImportacionAdmin(
        int filasProcesadas,
        int creados,
        int actualizados,
        int omitidos,
        List<String> errores) {

    public String resumen() {
        return "Filas procesadas: " + filasProcesadas
                + " · Creados: " + creados
                + " · Actualizados: " + actualizados
                + " · Omitidos: " + omitidos
                + " · Errores: " + errores.size();
    }
}
