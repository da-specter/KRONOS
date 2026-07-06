package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUDITORIA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_AUDITORIA", columnDefinition = "NUMBER(19,0)")
    private Long idAuditoria;

    // Set Null: Si se borra un usuario, el registro de auditoría persiste apuntando a NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", referencedColumnName = "ID_USUARIO", columnDefinition = "NUMBER(19,0)")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Usuario usuario;

    @Column(name = "DESCRIPCION", columnDefinition = "VARCHAR2(250)", nullable = false)
    private String descripcion;

    @Column(name = "FECHA", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCION", columnDefinition = "VARCHAR2(30)", nullable = false)
    private AccionAuditoria accion;

    @PrePersist
    protected void onPrePersist() {
        if (this.fecha == null) this.fecha = LocalDateTime.now();
    }
}