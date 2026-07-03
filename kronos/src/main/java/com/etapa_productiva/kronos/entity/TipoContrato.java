
package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "TIPO_CONTRATO", uniqueConstraints = {
    // Evita que se dupliquen las modalidades en el catálogo
    @UniqueConstraint(columnNames = "NOMBRE_TIPO_CONTRATO")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_TIPO_CONTRATO", columnDefinition = "NUMBER(19,0)")
    private Long idTipoContrato;

    @Column(name = "NOMBRE_TIPO_CONTRATO", columnDefinition = "VARCHAR2(50)", nullable = false)
    private String nombreTipoContrato;

    // Control de estado booleano mapeado como NUMBER(1,0) para Oracle DB
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // Inicialización automática del estado en Activo (true) antes de guardar en Oracle
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = true;
        }
    }
}