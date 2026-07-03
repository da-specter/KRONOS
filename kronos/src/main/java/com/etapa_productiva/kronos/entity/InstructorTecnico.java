package com.etapa_productiva.kronos.entity  ;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "INSTRUCTOR_TECNICO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorTecnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // GENERATED AS IDENTITY nativo de Oracle
    @Column(name = "ID_INSTRUCTOR_TECNICO", columnDefinition = "NUMBER(19,0)")
    private Long idInstructorTecnico;

    // Relación Uno a Uno: Vincula directamente con la cuenta base del Usuario
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_USUARIO", 
        referencedColumnName = "ID_USUARIO", 
        columnDefinition = "NUMBER(19,0)", 
        unique = true, 
        nullable = false
    )
    private Usuario usuario;

    // Control de estado booleano para activación/desactivación lógica (NUMBER(1,0) en Oracle)
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // Interceptor automático: Asegura que el registro nazca activo (true) antes del INSERT
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = true;
        }
    }
}