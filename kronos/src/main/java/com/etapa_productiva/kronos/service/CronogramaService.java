package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.DiaCalendarioDto;
import com.etapa_productiva.kronos.dto.MesCalendarioDto;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EstadoBitacora;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.repository.CronogramaBitacorasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 📅 Automatiza el calendario de seguimiento de la Etapa Productiva: genera y persiste,
 * de forma transaccional, las 12 bitácoras quincenales reglamentarias apenas el Coordinador
 * registra la Etapa Productiva de un aprendiz.
 */
@Service
public class CronogramaService {

    private static final int TOTAL_BITACORAS = 12;

    @Autowired
    private CronogramaBitacorasRepository cronogramaBitacorasRepository;

    /**
     * Distribuye las 12 fechas límite mediante desplazamientos relativos por mes
     * (.plusMonths()) en vez de incrementos lineales de 15 días, para no arrastrar
     * el desfase acumulativo de los meses de 28, 30 o 31 días:
     *   - Entregas impares (1,3,5,7,9,11): mes desplazado (i/2) + colchón fijo de 15 días.
     *   - Entregas pares (2,4,6,8,10): mes desplazado (i/2) exacto, sin colchón.
     *   - Entrega 12: se fija de forma dura e idéntica a la fechaFin de la Etapa Productiva.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<CronogramaBitacoras> generarCronograma(EtapaProductiva etapa) {
        LocalDate fechaInicio = etapa.getFechaInicio();
        LocalDate fechaFin = etapa.getFechaFin();

        List<CronogramaBitacoras> cronograma = new ArrayList<>(TOTAL_BITACORAS);
        LocalDate fechaApertura = fechaInicio;

        for (int i = 1; i <= TOTAL_BITACORAS; i++) {
            LocalDate fechaLimite;

            if (i == TOTAL_BITACORAS) {
                fechaLimite = fechaFin;
            } else if (i % 2 != 0) {
                fechaLimite = fechaInicio.plusMonths(i / 2).plusDays(15);
            } else {
                fechaLimite = fechaInicio.plusMonths(i / 2);
            }

            cronograma.add(CronogramaBitacoras.builder()
                    .etapaProductiva(etapa)
                    .numeroBitacora(i)
                    .fechaApertura(fechaApertura)
                    .fechaLimite(fechaLimite)
                    .estado(EstadoBitacora.PENDIENTE)
                    .build());

            // El buzón de la siguiente entrega abre justo donde cerró la anterior
            fechaApertura = fechaLimite;
        }

        return cronogramaBitacorasRepository.saveAll(cronograma);
    }

    /**
     * 🩹 Autorreparación: si la Etapa Productiva ya tiene su cronograma generado lo devuelve tal cual;
     * si no lo tiene (por ejemplo, etapas que quedaron activas antes de automatizar este proceso),
     * lo genera en este mismo momento en vez de dejar al aprendiz sin fechas de entrega.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<CronogramaBitacoras> obtenerOGenerarCronograma(EtapaProductiva etapa) {
        List<CronogramaBitacoras> existente =
                cronogramaBitacorasRepository.findByEtapaProductivaIdEtapaOrderByNumeroBitacoraAsc(etapa.getIdEtapa());

        if (!existente.isEmpty()) {
            return existente;
        }

        return generarCronograma(etapa);
    }

    /**
     * 🗓️ Arma el calendario mensual (uno por cada mes entre fechaInicio y fechaFin) ya organizado
     * en semanas de 7 días, con las celdas de entrega de bitácora resueltas, para que la vista solo
     * tenga que pintar la grilla sin hacer aritmética de fechas en Thymeleaf.
     */
    public List<MesCalendarioDto> construirCalendario(LocalDate fechaInicio, LocalDate fechaFin, List<CronogramaBitacoras> cronograma) {
        Map<LocalDate, CronogramaBitacoras> entregasPorFecha = cronograma.stream()
                .collect(Collectors.toMap(CronogramaBitacoras::getFechaLimite, c -> c, (a, b) -> a));

        Locale localeEs = Locale.of("es", "CO");
        List<MesCalendarioDto> meses = new ArrayList<>();

        LocalDate cursorMes = fechaInicio.withDayOfMonth(1);
        LocalDate ultimoMes = fechaFin.withDayOfMonth(1);
        while (!cursorMes.isAfter(ultimoMes)) {
            meses.add(construirMes(cursorMes, entregasPorFecha, localeEs));
            cursorMes = cursorMes.plusMonths(1);
        }

        return meses;
    }

    private MesCalendarioDto construirMes(LocalDate primerDiaMes, Map<LocalDate, CronogramaBitacoras> entregasPorFecha, Locale localeEs) {
        LocalDate hoy = LocalDate.now();
        int diasEnMes = primerDiaMes.lengthOfMonth();
        int corrimientoInicial = primerDiaMes.getDayOfWeek().getValue() - 1; // Lunes = 0 ... Domingo = 6

        List<List<DiaCalendarioDto>> semanas = new ArrayList<>();
        List<DiaCalendarioDto> semanaActual = new ArrayList<>();

        for (int i = 0; i < corrimientoInicial; i++) {
            semanaActual.add(DiaCalendarioDto.builder().delMesActual(false).build());
        }

        for (int dia = 1; dia <= diasEnMes; dia++) {
            LocalDate fecha = primerDiaMes.withDayOfMonth(dia);
            semanaActual.add(DiaCalendarioDto.builder()
                    .numeroDia(dia)
                    .fecha(fecha)
                    .delMesActual(true)
                    .hoy(fecha.isEqual(hoy))
                    .entrega(entregasPorFecha.get(fecha))
                    .build());

            if (semanaActual.size() == 7) {
                semanas.add(semanaActual);
                semanaActual = new ArrayList<>();
            }
        }

        if (!semanaActual.isEmpty()) {
            while (semanaActual.size() < 7) {
                semanaActual.add(DiaCalendarioDto.builder().delMesActual(false).build());
            }
            semanas.add(semanaActual);
        }

        String nombreMes = primerDiaMes.getMonth().getDisplayName(TextStyle.FULL, localeEs);
        nombreMes = Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1);

        return MesCalendarioDto.builder()
                .nombreMes(nombreMes + " " + primerDiaMes.getYear())
                .semanas(semanas)
                .build();
    }
}
