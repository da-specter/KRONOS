package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    // El vínculo con la Etapa no es directo: BITACORA -> CRONOGRAMA_BITACORAS -> ETAPA_PRODUCTIVA
    @Query(value = "SELECT b.* FROM bitacora b " +
                   "JOIN cronograma_bitacoras c ON b.id_cronograma = c.id_cronograma " +
                   "WHERE c.id_etapa = :idEtapa ORDER BY b.fecha_entrega DESC", nativeQuery = true)
    List<Bitacora> findByEtapaProductivaIdEtapaOrderByFechaEntregaDesc(@Param("idEtapa") Long idEtapa);
}