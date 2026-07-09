package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.DashboardCoordinacionDto;
import com.etapa_productiva.kronos.dto.DashboardCoordinacionDto.FichaResumen;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.FichaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 🎓 Módulo de Coordinación Académica: dashboard de solo lectura con los aprendices agrupados
 * por ficha, mostrando cuántos están en Etapa Productiva y cuántos aún no.
 */
@Service
public class CoordinacionAcademicaService {

    @Autowired
    private FichaRepository fichaRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Transactional(readOnly = true)
    public DashboardCoordinacionDto calcularDashboard() {
        List<FichaResumen> fichas = new ArrayList<>();
        int totalAprendices = 0;
        int totalEnEtapa = 0;

        for (Ficha ficha : fichaRepository.findAll()) {
            List<AprendizFicha> matriculas = aprendizFichaRepository.findByFichaIdFicha(ficha.getIdFicha());
            if (matriculas.isEmpty()) continue;

            Set<Long> matriculasConEtapa = new HashSet<>();
            for (EtapaProductiva etapa : etapaProductivaRepository.findByAprendizFichaFichaIdFicha(ficha.getIdFicha())) {
                matriculasConEtapa.add(etapa.getAprendizFicha().getIdAprendizFicha());
            }

            int total = matriculas.size();
            int enEtapa = (int) matriculas.stream()
                    .filter(m -> matriculasConEtapa.contains(m.getIdAprendizFicha()))
                    .count();
            int sinEtapa = total - enEtapa;

            fichas.add(FichaResumen.builder()
                    .numeroFicha(ficha.getNumeroFicha())
                    .programa(ficha.getProgramaFormacion().getNombrePrograma())
                    .enEtapa(enEtapa)
                    .sinEtapa(sinEtapa)
                    .total(total)
                    .porcentajeEnEtapa((int) Math.round(enEtapa * 100.0 / total))
                    .porcentajeSinEtapa((int) Math.round(sinEtapa * 100.0 / total))
                    .build());

            totalAprendices += total;
            totalEnEtapa += enEtapa;
        }

        fichas.sort(Comparator.comparing(FichaResumen::getNumeroFicha));

        int porcentajeEnEtapaGlobal = totalAprendices > 0 ? (int) Math.round(totalEnEtapa * 100.0 / totalAprendices) : 0;
        String donutGradientGlobal = totalAprendices > 0
                ? "conic-gradient(#057015 0% " + porcentajeEnEtapaGlobal + "%, #D97706 " + porcentajeEnEtapaGlobal + "% 100%)"
                : "conic-gradient(#E5E7EB 0% 100%)";

        return DashboardCoordinacionDto.builder()
                .totalFichas(fichas.size())
                .totalAprendices(totalAprendices)
                .totalEnEtapa(totalEnEtapa)
                .totalSinEtapa(totalAprendices - totalEnEtapa)
                .porcentajeEnEtapaGlobal(porcentajeEnEtapaGlobal)
                .donutGradientGlobal(donutGradientGlobal)
                .fichas(fichas)
                .build();
    }
}
