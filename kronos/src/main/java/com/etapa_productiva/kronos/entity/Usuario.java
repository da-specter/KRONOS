package com.etapa_productiva.kronos.entity; 

import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "USUARIO", uniqueConstraints = {
    @UniqueConstraint(columnNames = "DOCUMENTO"),
    @UniqueConstraint(columnNames = "CORREO")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  
    @Column(name = "ID_USUARIO", columnDefinition = "NUMBER(19,0)")
    private Long idUsuario;


   @Enumerated(EnumType.STRING)
@Column(name = "TIPO_DOCUMENTO", columnDefinition = "VARCHAR2(10)", nullable = false)
private TipoDocumento tipoDocumento; 

    @Column(name = "DOCUMENTO", columnDefinition = "VARCHAR2(10)", nullable = false)
    private String documento;

    @Column(name = "NOMBRE", columnDefinition = "VARCHAR2(30)", nullable = false)
    private String nombre;

    @Column(name = "APELLIDO", columnDefinition = "VARCHAR2(30)", nullable = false)
    private String apellido;

    @Column(name = "CORREO", columnDefinition = "VARCHAR2(150)", nullable = false)
    private String correoElectronico;

    @Column(name = "PASSWORD", columnDefinition = "VARCHAR2(255)", nullable = false)
    private String password;

    @Column(name = "TELEFONO", columnDefinition = "VARCHAR2(11)")
    private String telefono;

    // 🖼️ Ruta pública (bajo /uploads/**) de la foto de perfil, disponible para cualquier rol
    @Column(name = "FOTO_PERFIL", columnDefinition = "VARCHAR2(255)")
    private String fotoPerfil;

    // Mapeo estricto del booleano como NUMBER(1,0) para Oracle
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // 🔐 Todo usuario nuevo arranca con contraseña = su documento; esta bandera obliga a
    // cambiarla en el primer login (ver CambioContrasenaInterceptor). Por defecto en FALSE
    // para no afectar cuentas existentes: solo se enciende explícitamente al crear el usuario.
    @Column(name = "DEBE_CAMBIAR_CONTRASENA", columnDefinition = "NUMBER(1,0) DEFAULT 0", nullable = false)
    private Boolean debeCambiarContrasena;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UsuarioRol> usuarioRoles;

    @PrePersist
    protected void onPrePersist() {  // se ejecuta antes de insertar un nuevo registro
        if (this.estado == null) {
            this.estado = true;
        }
        if (this.debeCambiarContrasena == null) {
            this.debeCambiarContrasena = false;
        }
    }
}