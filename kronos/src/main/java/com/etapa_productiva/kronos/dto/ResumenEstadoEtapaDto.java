package com.etapa_productiva.kronos.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 🚦 Conteo agregado del semáforo de "Gestión Aprendices" (EstadoEtapaAprendiz): cuántos
 * aprendices hay en cada punto de la línea de tiempo de su Etapa Práctica. Antes solo existía
 * la clasificación fila por fila; este resumen es lo que permite responder de un vistazo
 * "¿cuántos terminan en un mes o menos?" sin tener que contar la tabla a mano.
 */
@Data
@Builder
public class ResumenEstadoEtapaDto {
    private long terminoContrato;
    private long epPorTerminar;
    private long enEp;
    private long iniciaEpPronto;
    private long faltaMasDeUnMes;
    private long total;
}
