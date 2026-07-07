package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.TipoContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TipoContratoRepository extends JpaRepository<TipoContrato, Long> {
    // Evita duplicar el mismo tipo de contrato cuando el Gestor lo escribe libremente en el formulario
    Optional<TipoContrato> findByNombreTipoContratoIgnoreCase(String nombreTipoContrato);

    // Catálogo de modalidades activas para el select de "Tipo de contrato" del Registro de Etapa
    java.util.List<TipoContrato> findByEstadoTrueOrderByNombreTipoContratoAsc();
}
