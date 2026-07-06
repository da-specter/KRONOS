package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📅 Agenda de visitas de seguimiento agrupada en las 3 franjas que pide la vista:
 * pasadas, pendientes (hoy) y futuras.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitasAgendaResumenDto {
    private List<VisitaAgendaDto> pasadas;
    private List<VisitaAgendaDto> pendientes;
    private List<VisitaAgendaDto> futuras;
}
