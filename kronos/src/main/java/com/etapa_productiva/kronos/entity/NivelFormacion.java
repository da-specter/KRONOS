package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "NIVEL_FORMACION", uniqueConstraints = {
    @UniqueConstraint(columnNames = "NOMBRE_NIVEL")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NivelFormacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Autoincremental nativo con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_NIVEL", columnDefinition = "NUMBER(19,0)")
    private Long idNivel;

    @Column(name = "NOMBRE_NIVEL", columnDefinition = "VARCHAR2(40)", nullable = false)
    private String nombreNivel;

    // Control de estado booleano mapeado como NUMBER(1,0) para Oracle DB
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // Interceptador para asegurar que cada nuevo nivel registrado inicie como Activo (true)
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = true;
        }
    }
}
