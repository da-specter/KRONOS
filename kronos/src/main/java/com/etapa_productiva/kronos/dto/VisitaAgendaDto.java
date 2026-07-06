package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📅 Fila de la agenda de visitas de seguimiento (usada tanto por el Instructor de
 * Seguimiento como por el Aprendiz en su cronograma): ya trae el texto listo para pintar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitaAgendaDto {
    private Long idVisita;
    private String fecha;
    private String aprendizNombre;
    private String ficha;
    private String tipoEtiqueta;
    private String modalidadEtiqueta;
    private String estado; // nombre crudo del enum (para lógica en Java)
    private String estadoEtiqueta; // "Planeada"/"Realizada"/"Aplazada"/"Cancelada" (para pintar)
    private String novedad;
    private boolean puedeGestionar; // true si el instructor aún puede cancelar/aplazar esta visita
    private String evidenciaRuta; // ruta pública del archivo de evidencia (solo si estado == REALIZADA)
}
