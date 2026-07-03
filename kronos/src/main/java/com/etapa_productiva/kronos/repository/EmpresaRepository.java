package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    // Evita duplicar el registro de empresas aliadas mediante el NIT
    Optional<Empresa> findByNit(String nit);
}