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

    // El vínculo con el Usuario no es directo: ETAPA_PRODUCTIVA -> APRENDIZ_FICHA -> USUARIO
    @Query(value = "SELECT e.* FROM etapa_productiva e " +
                   "JOIN aprendiz_ficha af ON e.id_aprendiz_ficha = af.id_aprendiz_ficha " +
                   "WHERE af.id_usuario = :idUsuario", nativeQuery = true)
    Optional<EtapaProductiva> findByAprendizIdUsuario(@Param("idUsuario") Long idUsuario);

    // 🗂️ Bandeja del Gestor de Etapa: aprendices con Etapa Productiva activa que aún no tienen
    // una asignación VIGENTE en ASIGNACION_INSTRUCTOR_ETAPA (ESTADO_ASIGNACION = 1).
    @Query("SELECT e FROM EtapaProductiva e WHERE e.estadoEtapa = :estadoEtapa AND e.idEtapa NOT IN " +
           "(SELECT a.etapaProductiva.idEtapa FROM AsignacionInstructorEtapa a WHERE a.estadoAsignacion = true)")
    List<EtapaProductiva> findSinInstructorSeguimientoAsignado(@Param("estadoEtapa") EstadoEtapa estadoEtapa);

    // 📋 Gestión de Fichas: todas las Etapas Productivas de los aprendices matriculados en una ficha
    List<EtapaProductiva> findByAprendizFichaFichaIdFicha(Long idFicha);
}