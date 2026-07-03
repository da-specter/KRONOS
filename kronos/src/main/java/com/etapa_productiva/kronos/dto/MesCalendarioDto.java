package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 🗓️ Un mes completo del calendario de "Mi Cronograma", ya organizado en semanas de 7 días
 * (lunes a domingo) listas para pintar en la grilla del Thymeleaf sin lógica de fechas en la vista.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesCalendarioDto {
    private String nombreMes;
    private List<List<DiaCalendarioDto>> semanas;
}
