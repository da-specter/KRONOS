package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {
    // Carga los municipios dependientes de un departamento en los selects del front
    List<Municipio> findByDepartamentoIdDepartamento(Long idDepartamento);

    // Evita duplicar el mismo municipio cuando el Gestor lo escribe libremente en el formulario
    Optional<Municipio> findByNombreMunicipioIgnoreCaseAndDepartamentoIdDepartamento(String nombreMunicipio, Long idDepartamento);

    // Importación de visitas: el Excel institucional no trae el Departamento, solo el nombre del
    // municipio; se busca por nombre en cualquier departamento (debe existir ya en el catálogo).
    Optional<Municipio> findFirstByNombreMunicipioIgnoreCase(String nombreMunicipio);
}