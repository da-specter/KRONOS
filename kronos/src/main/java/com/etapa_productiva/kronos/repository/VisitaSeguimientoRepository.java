package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VisitaSeguimientoRepository extends JpaRepository<VisitaSeguimiento, Long> {
    List<VisitaSeguimiento> findByEtapaProductivaIdEtapaOrderByFechaVisitaDesc(Long idEtapa);

    // 📅 Agenda del Instructor de Seguimiento (todas las visitas que él mismo programó)
    List<VisitaSeguimiento> findByInstructorIdUsuarioOrderByFechaVisitaAsc(Long idUsuario);

    // 📅 Agenda del Aprendiz en su cronograma (visitas de su propia Etapa Productiva)
    List<VisitaSeguimiento> findByEtapaProductivaIdEtapaOrderByFechaVisitaAsc(Long idEtapa);

    // ⏰ Job diario de alertas: visitas aún planeadas que caen exactamente en la fecha objetivo (hoy+3 o hoy+2)
    List<VisitaSeguimiento> findByEstadoVisitaAndFechaVisita(EstadoVisita estadoVisita, LocalDate fechaVisita);
}