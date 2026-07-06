package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    // 📊 Dashboard del Administrador: total de usuarios activos que tienen un rol específico (Ej: APRENDIZ)
    long countByRolNombreRolAndUsuarioEstadoTrue(String nombreRol);

    // 👥 Usuarios del Sistema: asignaciones de rol de un usuario puntual
    List<UsuarioRol> findByUsuarioIdUsuario(Long idUsuario);

    boolean existsByUsuarioIdUsuarioAndRolIdRol(Long idUsuario, Long idRol);
}