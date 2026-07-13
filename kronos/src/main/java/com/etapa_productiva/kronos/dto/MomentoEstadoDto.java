package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 🧩 Estado de uno de los 3 "momentos" del Formato 023 para una Etapa Productiva: si ya se
 * habilitó (sus 4 bitácoras del grupo están ENTREGADA y el momento anterior ya quedó completo
 * por ambos lados), qué llevan diligenciado el aprendiz y el instructor, y si cada uno ya
 * terminó su parte (el momento completo = ambos lados listos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentoEstadoDto {
    private int numero; // 1, 2 o 3
    private boolean habilitado;
    private boolean aprendizCompleto;
    private boolean instructorCompleto;
    private int bitacoraDesde;
    private int bitacoraHasta;

    // Lado instructor
    private String observacion;
    private String juicioEvaluacion; // solo Momento 3: "APROBADO" | "NO_APROBADO" | null
    private String fechaRegistro;
    private String retroInstructorProceso;    // solo Momento 3
    private String retroInstructorDesempeno;  // solo Momento 3

    // Lado aprendiz
    private String observacionAprendiz;
    private String competenciasDesarrollar;   // solo Momento 1
    private String resultadosAprendizaje;     // solo Momento 1
    private String actividadesDesarrollar;    // solo Momento 1
    private String evidenciaDescripcion;      // solo Momento 1
    private String evidenciaRutaArchivo;      // solo Momento 1
    private String fechaMomento;              // solo Momento 2, ISO yyyy-MM-dd
    private String observacionEnteCoformador; // solo Momento 2, opcional
    private String retroEnteProceso;          // solo Momento 3, opcional
    private String retroEnteDesempeno;        // solo Momento 3, opcional
    private String retroAprendizProceso;      // solo Momento 3
    private String retroAprendizDesempeno;    // solo Momento 3

    // Factores Técnicos/Actitudinales (solo Momento 2 y 3)
    private List<FactorEstadoDto> factoresTecnicos;
    private List<FactorEstadoDto> factoresActitudinales;

    // Comunes
    private String enlaceGrabacion;
    private String ciudad;
    private String fechaDiligenciamiento; // dd/MM/yyyy
    private String modalidadFirma;        // "PRESENCIAL" | "VIRTUAL"
}
