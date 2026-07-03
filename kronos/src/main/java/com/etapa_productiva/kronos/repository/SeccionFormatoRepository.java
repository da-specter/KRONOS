package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.SeccionFormato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SeccionFormatoRepository extends JpaRepository<SeccionFormato, Long> {
    List<SeccionFormato> findByEstadoTrue();
}