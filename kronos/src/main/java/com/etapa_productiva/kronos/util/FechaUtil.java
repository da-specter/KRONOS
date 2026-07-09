package com.etapa_productiva.kronos.util;

import java.time.LocalDateTime;

/**
 * 📅 Agrupación y presentación de fechas por mes calendario, usado en los filtros
 * "por mes" de Gestión Aprendices y Mis Aprendices (Instructor de Seguimiento).
 */
public final class FechaUtil {

    private static final String[] MESES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private FechaUtil() {
    }

    /** Clave ordenable "yyyy-MM" (ej. "2026-07"); null si la fecha es null. */
    public static String claveMes(LocalDateTime fecha) {
        return fecha == null ? null : String.format("%04d-%02d", fecha.getYear(), fecha.getMonthValue());
    }

    /** Etiqueta legible en español para una clave "yyyy-MM" (ej. "Julio 2026"). */
    public static String etiquetaMes(String claveMes) {
        String[] partes = claveMes.split("-");
        int mes = Integer.parseInt(partes[1]);
        return MESES[mes - 1] + " " + partes[0];
    }
}
