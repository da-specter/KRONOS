package com.etapa_productiva.kronos.dto;

import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 🗓️ Una celda del calendario mensual de "Mi Cronograma": puede ser un día real del mes
 * (con o sin entrega de bitácora) o una celda vacía de relleno para alinear la grilla.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaCalendarioDto {
    private int numeroDia;
    private LocalDate fecha;
    private boolean delMesActual;
    private boolean hoy;
    private CronogramaBitacoras entrega;
}
