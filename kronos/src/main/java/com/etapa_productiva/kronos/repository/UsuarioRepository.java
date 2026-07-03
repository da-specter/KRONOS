package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

// 1. UsuarioRepository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Útil para el inicio de sesión (Login)
    Optional<Usuario> findByCorreoElectronico(String correoElectronico);

    // Panel del Administrador: conteo de usuarios activos/inactivos
    long countByEstado(Boolean estado);

    // Destinatario por defecto de las Novedades que radica el Aprendiz: cualquier Gestor de Etapa activo
    @Query(value = "SELECT u.* FROM usuario u " +
                   "JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario " +
                   "JOIN rol r ON r.id_rol = ur.id_rol " +
                   "WHERE r.nombre_rol = 'GESTOR_ETAPA' AND u.estado = 1 " +
                   "FETCH FIRST 1 ROWS ONLY", nativeQuery = true)
    Optional<Usuario> findPrimerGestorEtapaActivo();

    // Notifica a TODOS los Gestores de Etapa activos cuando un aprendiz radica una nueva solicitud
    @Query(value = "SELECT u.* FROM usuario u " +
                   "JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario " +
                   "JOIN rol r ON r.id_rol = ur.id_rol " +
                   "WHERE r.nombre_rol = 'GESTOR_ETAPA' AND u.estado = 1", nativeQuery = true)
    List<Usuario> findAllGestoresEtapaActivos();
}

