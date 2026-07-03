package com.etapa_productiva.kronos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Entity
@Table(name = "FICHA", uniqueConstraints = {
    @UniqueConstraint(columnNames = "NUMERO_FICHA")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ficha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Se alinea con GENERATED AS IDENTITY de Oracle
    @Column(name = "ID_FICHA", columnDefinition = "NUMBER(19,0)")
    private Long idFicha;

    // En el SENA el número de ficha suele ser de 7 dígitos, lo dejamos con VARCHAR2 por flexibilidad con códigos alfanuméricos especiales
    @Column(name = "NUMERO_FICHA", columnDefinition = "VARCHAR2(7)", nullable = false)
    private String numeroFicha;

    // Usamos LocalDate ya que para el inicio y fin de la ficha solo nos importan Año-Mes-Día (sin horas)
    @Column(name = "FECHA_INICIO", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "FECHA_FIN", columnDefinition = "DATE", nullable = false)
    private LocalDate fechaFin;

    // Control de estado booleano mapeado como NUMBER(1,0) para Oracle DB
    @Column(name = "ESTADO", columnDefinition = "NUMBER(1,0) DEFAULT 1", nullable = false)
    private Boolean estado;

    // Relación Muchos a Uno: Muchas fichas pertenecen al mismo Programa de Formación (Ej: Varias fichas de ADSO)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ID_PROGRAMA", 
        referencedColumnName = "ID_PROGRAMA", 
        columnDefinition = "NUMBER(19,0)", 
        nullable = false
    )
    private ProgramasFormacion programaFormacion;

    // Asegura que la ficha inicie como Activa por defecto antes de ir a Oracle
    @PrePersist
    protected void onPrePersist() {
        if (this.estado == null) {
            this.estado = true;
        }
    }
}