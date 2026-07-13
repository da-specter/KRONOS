package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.EstadoVisita;
import com.etapa_productiva.kronos.entity.VisitaSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitaSeguimientoRepository extends JpaRepository<VisitaSeguimiento, Long> {
    List<VisitaSeguimiento> findByEtapaProductivaIdEtapaOrderByFechaVisitaDesc(Long idEtapa);

    // 📅 Agenda del Instructor de Seguimiento (todas las visitas que él mismo programó)
    List<VisitaSeguimiento> findByInstructorIdUsuarioOrderByFechaVisitaAsc(Long idUsuario);

    // 📅 Agenda del Aprendiz en su cronograma (visitas de su propia Etapa Productiva)
    List<VisitaSeguimiento> findByEtapaProductivaIdEtapaOrderByFechaVisitaAsc(Long idEtapa);

    // ⏰ Job diario de alertas: visitas aún planeadas dentro de la ventana [hoy, hoy+N] que
    // todavía no recibieron su alerta. Usar un rango (no la fecha exacta) permite que el job
    // se ponga al día si no corrió el día puntual en que faltaban exactamente N días; la
    // bandera evita reenviar la misma alerta si el job se ejecuta más de una vez.
    List<VisitaSeguimiento> findByEstadoVisitaAndFechaVisitaBetweenAndAlertaInstructorEnviadaFalse(
            EstadoVisita estadoVisita, LocalDateTime desde, LocalDateTime hasta);

    List<VisitaSeguimiento> findByEstadoVisitaAndFechaVisitaBetweenAndAlertaAprendizEnviadaFalse(
            EstadoVisita estadoVisita, LocalDateTime desde, LocalDateTime hasta);

    // Para saber si una Etapa Productiva ya tiene al menos una visita agendada (primera visita)
    boolean existsByEtapaProductivaIdEtapa(Long idEtapa);
}