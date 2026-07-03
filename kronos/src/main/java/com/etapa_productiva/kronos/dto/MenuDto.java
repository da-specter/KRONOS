package com.etapa_productiva.kronos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuDto {
    private String nombre;
    private String ruta;

    // Sub-ítems del módulo: cuando no es null/vacío, el ítem se pinta como un
    // grupo desplegable en el sidebar (Ej: "Gestión Etapa" → Fichas / Aprendices)
    private List<MenuDto> hijos;

    public MenuDto(String nombre, String ruta) {
        this.nombre = nombre;
        this.ruta = ruta;
    }

    public boolean isGrupo() {
        return hijos != null && !hijos.isEmpty();
    }
}
