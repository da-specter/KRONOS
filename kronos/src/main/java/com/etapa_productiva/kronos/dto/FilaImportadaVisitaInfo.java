package com.etapa_productiva.kronos.dto;

/**
 * 📥 Qué entidades resultaron nuevas al procesar una fila del Excel de importación de
 * "Visitas de Seguimiento" (usado para acumular los contadores del resumen final).
 */
public record FilaImportadaVisitaInfo(
        boolean usuarioCreado,
        boolean fichaCreada,
        boolean programaCreado,
        boolean empresaCreada,
        boolean tipoContratoCreado,
        boolean etapaCreada,
        boolean visitaCreada,
        boolean omitida
) {
    public static final FilaImportadaVisitaInfo OMITIDA = new FilaImportadaVisitaInfo(
            false, false, false, false, false, false, false, true);
}
