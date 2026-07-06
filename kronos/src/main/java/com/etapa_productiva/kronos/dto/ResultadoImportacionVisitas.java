package com.etapa_productiva.kronos.dto;

import java.util.List;

/**
 * 📥 Resultado resumido de la importación masiva de "Visitas de Seguimiento": cuántas
 * entidades se crearon en cada nivel, cuántas filas se omitieron por ya existir, y el
 * detalle de los errores (fila + motivo) para que el Instructor sepa exactamente qué revisar.
 */
public record ResultadoImportacionVisitas(
        int filas,
        int usuariosCreados,
        int fichasCreadas,
        int programasCreados,
        int empresasCreadas,
        int tiposContratoCreados,
        int etapasCreadas,
        int visitasCreadas,
        int omitidas,
        List<String> errores
) {
}
