package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.ConfiguracionGlobal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracionGlobalRepository extends JpaRepository<ConfiguracionGlobal, Long> {

    // Lectura "en caliente" de una variable puntual del sistema (Ej: DIAS_ALERTA_APRENDIZ)
    Optional<ConfiguracionGlobal> findByClave(String clave);

    // Listado ordenado para la pantalla de Configuración Global del Administrador
    List<ConfiguracionGlobal> findAllByOrderByClaveAsc();
}
