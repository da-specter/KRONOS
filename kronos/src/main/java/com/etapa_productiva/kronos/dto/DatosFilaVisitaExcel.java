package com.etapa_productiva.kronos.dto;

/**
 * 📥 Texto crudo (sin parsear) leído de una fila del Excel institucional de "Visitas de
 * Seguimiento", antes de resolverlo contra las entidades/catálogos de KRONOS.
 */
public record DatosFilaVisitaExcel(
        String documento,
        String tipoDocumento,
        String nombreCompleto,
        String numeroFicha,
        String nivelFormacion,
        String programaFormacion,
        String tipoContrato,
        String fechaTerminacion,
        String nitEmpresa,
        String razonSocial,
        String direccionEmpresa,
        String municipio,
        String jefeNombre,
        String jefeCorreo,
        String jefeTelefono,
        String modalidadVisita,
        String tipoVisita,
        String fechaVisita,
        String numeroActa
) {
}
