package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.dto.AprendizCertificacionDto;
import com.etapa_productiva.kronos.dto.CertificacionInfoDto;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.EstadoAcademico;
import com.etapa_productiva.kronos.entity.EstadoEtapa;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.AsignacionInstructorEtapaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 🎓 Módulo "Certificación Aprendiz" del Gestor de Etapa: bandeja de aprendices cuya Etapa
 * Productiva ya está en POR_CERTIFICAR (100% de bitácoras + Formato 023 aprobados) — muestra
 * su resumen de requisitos y su Instructor de Seguimiento, y permite dar la aprobación final
 * que certifica su Etapa Productiva.
 */
@Service
public class CertificacionAprendizService {

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private AsignacionInstructorEtapaRepository asignacionInstructorEtapaRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private EvaluacionFormatosService evaluacionFormatosService;

    @Autowired
    private NotificacionService notificacionService;

    private static final String SIN_ASIGNAR = "Sin asignar";

    /** Aprendices con Etapa Productiva en POR_CERTIFICAR, listos para la revisión del Gestor. */
    @Transactional(readOnly = true)
    public List<AprendizCertificacionDto> listarPorCertificar() {
        List<AprendizCertificacionDto> filas = new ArrayList<>();

        for (EtapaProductiva etapa : etapaProductivaRepository.findByEstadoEtapaOrderByFechaInicioAsc(EstadoEtapa.POR_CERTIFICAR)) {
            Usuario aprendiz = etapa.getAprendizFicha().getUsuario();
            CertificacionInfoDto info = evaluacionFormatosService.calcularInfoCertificacion(etapa);

            String instructor = asignacionInstructorEtapaRepository
                    .findByEtapaProductivaIdEtapaAndEstadoAsignacionTrue(etapa.getIdEtapa())
                    .map(a -> a.getInstructor().getUsuario())
                    .map(u -> u.getNombre() + " " + u.getApellido())
                    .orElse(SIN_ASIGNAR);

            filas.add(AprendizCertificacionDto.builder()
                    .idEtapa(etapa.getIdEtapa())
                    .nombres(aprendiz.getNombre())
                    .apellidos(aprendiz.getApellido())
                    .documento(aprendiz.getDocumento())
                    .ficha(etapa.getAprendizFicha().getFicha().getNumeroFicha())
                    .totalBitacoras(info.getTotalBitacoras())
                    .bitacorasAprobadas(info.getBitacorasAprobadas())
                    .formatoAprobado(info.isFormatoAprobado())
                    .visitasRealizadas(info.getVisitasRealizadas())
                    .totalVisitas(info.getTotalVisitas())
                    .instructorSeguimiento(instructor)
                    .build());
        }

        filas.sort(Comparator.comparing(AprendizCertificacionDto::getApellidos, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AprendizCertificacionDto::getNombres, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    /** ✅ Aprobación final: certifica la Etapa Productiva y notifica al aprendiz. */
    @Transactional
    public void certificar(Long idEtapa) {
        EtapaProductiva etapa = etapaProductivaRepository.findById(idEtapa)
                .orElseThrow(() -> new IllegalArgumentException("La etapa productiva indicada no existe."));

        if (etapa.getEstadoEtapa() != EstadoEtapa.POR_CERTIFICAR) {
            throw new IllegalStateException("Este aprendiz no está en estado POR CERTIFICAR.");
        }

        etapa.setEstadoEtapa(EstadoEtapa.CERTIFICADO);
        etapaProductivaRepository.save(etapa);

        AprendizFicha aprendizFicha = etapa.getAprendizFicha();
        aprendizFicha.setEstadoAcademico(EstadoAcademico.CERTIFICADO);
        aprendizFichaRepository.save(aprendizFicha);

        Usuario aprendiz = aprendizFicha.getUsuario();
        notificacionService.crear(aprendiz,
                "🏆 ¡Estás CERTIFICADO(A)! Tu expediente fue aprobado por la Coordinación Académica. Ingresa a tu dashboard para ver el detalle.");
    }
}
