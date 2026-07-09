package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.EstadoEtapa;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EtapaProductivaRepository extends JpaRepository<EtapaProductiva, Long> {

    // El vínculo con el Usuario no es directo: ETAPA_PRODUCTIVA -> APRENDIZ_FICHA -> USUARIO.
    // Un aprendiz puede tener varias Etapas Productivas a lo largo del tiempo (si certifica una
    // y luego radica una nueva solicitud): se prioriza la que sigue vigente (no CERTIFICADO) y,
    // si todas ya fueron certificadas, se cae a la más reciente (para poder seguir mostrando su
    // historial/tarjeta de certificación mientras no exista un nuevo ciclo activo).
    @Query(value = "SELECT * FROM (" +
                   "  SELECT e.* FROM etapa_productiva e " +
                   "  JOIN aprendiz_ficha af ON e.id_aprendiz_ficha = af.id_aprendiz_ficha " +
                   "  WHERE af.id_usuario = :idUsuario " +
                   "  ORDER BY CASE WHEN e.estado_etapa = 'TERMINADO' THEN 1 ELSE 0 END, e.id_etapa DESC" +
                   ") WHERE ROWNUM = 1", nativeQuery = true)
    Optional<EtapaProductiva> findByAprendizIdUsuario(@Param("idUsuario") Long idUsuario);

    // 🗂️ Bandeja del Gestor de Etapa: aprendices con Etapa Productiva activa que aún no tienen
    // una asignación VIGENTE en ASIGNACION_INSTRUCTOR_ETAPA (ESTADO_ASIGNACION = 1).
    @Query("SELECT e FROM EtapaProductiva e WHERE e.estadoEtapa = :estadoEtapa AND e.idEtapa NOT IN " +
           "(SELECT a.etapaProductiva.idEtapa FROM AsignacionInstructorEtapa a WHERE a.estadoAsignacion = true)")
    List<EtapaProductiva> findSinInstructorSeguimientoAsignado(@Param("estadoEtapa") EstadoEtapa estadoEtapa);

    // 📋 Gestión de Fichas: todas las Etapas Productivas de los aprendices matriculados en una ficha
    List<EtapaProductiva> findByAprendizFichaFichaIdFicha(Long idFicha);

    // 📊 Dashboard del Administrador: aprendices actualmente en etapa práctica (EN_PROGRESO)
    long countByEstadoEtapa(EstadoEtapa estadoEtapa);

    // 🎓 Job diario de certificación automática: etapas POR_CERTIFICAR que llevan ahí desde
    // antes del corte de 3 meses (la certificación final ya no la hace el Gestor de Etapa,
    // ocurre en Sofía Plus; KRONOS solo refleja el estado tras el plazo).
    List<EtapaProductiva> findByEstadoEtapaAndFechaPorCertificarBefore(EstadoEtapa estadoEtapa, LocalDateTime corte);

    // ⏰ Job diario de alertas: etapas activas cuya FECHA_INICIO ya superó los días configurados
    // (15 por defecto) sin que se les haya enviado todavía la alerta de "agenda la primera visita"
    List<EtapaProductiva> findByEstadoEtapaAndAlertaPrimeraVisitaEnviadaFalseAndFechaInicioLessThanEqual(
            EstadoEtapa estadoEtapa, LocalDate fecha);
}