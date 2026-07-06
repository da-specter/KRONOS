package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.EstadoEtapa;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
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
                   "  ORDER BY CASE WHEN e.estado_etapa = 'CERTIFICADO' THEN 1 ELSE 0 END, e.id_etapa DESC" +
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

    // 🎓 Bandeja "Certificación Aprendiz" del Gestor de Etapa: aprendices que ya completaron
    // el 100% de bitácoras + Formato 023 y están a la espera de la firma del paz y salvo
    List<EtapaProductiva> findByEstadoEtapaOrderByFechaInicioAsc(EstadoEtapa estadoEtapa);
}