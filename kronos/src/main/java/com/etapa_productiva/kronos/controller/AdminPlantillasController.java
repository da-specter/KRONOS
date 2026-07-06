package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AccionAuditoria;
import com.etapa_productiva.kronos.entity.PlantillaFormato;
import com.etapa_productiva.kronos.entity.VisibilidadDocumento;
import com.etapa_productiva.kronos.repository.PlantillaFormatoRepository;
import com.etapa_productiva.kronos.repository.SeccionFormatoRepository;
import com.etapa_productiva.kronos.service.AuditoriaService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 📁 Módulo "Gestión de Formatos → Plantillas Oficiales" del Administrador:
 * repositorio central de las versiones limpias de los formatos Word/Excel
 * (Bitácoras, concertación, ARL...). Los archivos se guardan en uploads/plantillas/
 * y quedan registrados en PLANTILLA_FORMATO.
 */
@Controller
@RequestMapping("/admin/plantillas")
public class AdminPlantillasController {

    private static final Path DIRECTORIO_PLANTILLAS = Path.of("uploads", "plantillas");

    @Autowired private PlantillaFormatoRepository plantillaFormatoRepository;
    @Autowired private SeccionFormatoRepository seccionFormatoRepository;
    @Autowired private AuditoriaService auditoriaService;

    @GetMapping
    public String verPlantillas(HttpSession session, Model model) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        model.addAttribute("usuario", admin);
        model.addAttribute("plantillas", plantillaFormatoRepository.findAll());
        model.addAttribute("secciones", seccionFormatoRepository.findByEstadoTrue());
        model.addAttribute("visibilidades", VisibilidadDocumento.values());
        return "admin-plantillas";
    }

    /** ⬆️ Sube una plantilla oficial nueva al repositorio central */
    @PostMapping(value = "/subir", consumes = "multipart/form-data")
    public String subirPlantilla(@RequestParam String nombreDocumento,
                                 @RequestParam VisibilidadDocumento visibilidad,
                                 @RequestParam(required = false) Long idSeccionFormato,
                                 @RequestParam("archivo") MultipartFile archivo,
                                 HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            if (nombreDocumento == null || nombreDocumento.isBlank()) {
                throw new IllegalArgumentException("El nombre del documento es obligatorio.");
            }

            PlantillaFormato plantilla = PlantillaFormato.builder()
                    .nombreDocumento(nombreDocumento.trim())
                    .visibilidad(visibilidad)
                    .seccionFormato(idSeccionFormato == null ? null
                            : seccionFormatoRepository.findById(idSeccionFormato)
                                    .orElseThrow(() -> new IllegalArgumentException("La sección de formato elegida no existe.")))
                    .rutaArchivoPlantilla(guardarArchivo(archivo))
                    .build();
            plantillaFormatoRepository.save(plantilla);

            auditoriaService.registrar(admin.getIdUsuario(), AccionAuditoria.INSERT,
                    "Plantilla oficial subida: " + plantilla.getNombreDocumento()
                            + " (" + archivo.getOriginalFilename() + ")");
            redirect.addFlashAttribute("exito", "Plantilla oficial subida correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/plantillas";
    }

    /** 🔄 Reemplaza el archivo de una plantilla existente por su versión más reciente */
    @PostMapping(value = "/actualizar", consumes = "multipart/form-data")
    public String actualizarVersion(@RequestParam Long idPlantilla,
                                    @RequestParam("archivo") MultipartFile archivo,
                                    HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            PlantillaFormato plantilla = plantillaFormatoRepository.findById(idPlantilla)
                    .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe."));

            plantilla.setRutaArchivoPlantilla(guardarArchivo(archivo));
            plantilla.setFechaSubida(LocalDateTime.now());
            plantillaFormatoRepository.save(plantilla);

            auditoriaService.registrar(admin.getIdUsuario(), AccionAuditoria.UPDATE,
                    "Nueva versión de la plantilla oficial: " + plantilla.getNombreDocumento()
                            + " (" + archivo.getOriginalFilename() + ")");
            redirect.addFlashAttribute("exito", "Versión de la plantilla actualizada correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/plantillas";
    }

    /** 🔘 Activa o desactiva una plantilla sin borrar su historial */
    @PostMapping("/estado")
    public String cambiarEstado(@RequestParam Long idPlantilla,
                                @RequestParam boolean activar,
                                HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = AdminDatosMaestrosController.validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            PlantillaFormato plantilla = plantillaFormatoRepository.findById(idPlantilla)
                    .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe."));
            plantilla.setEstado(activar);
            plantillaFormatoRepository.save(plantilla);

            auditoriaService.registrar(admin.getIdUsuario(), AccionAuditoria.UPDATE,
                    "Plantilla oficial " + (activar ? "activada" : "desactivada") + ": " + plantilla.getNombreDocumento());
            redirect.addFlashAttribute("exito", "Plantilla " + (activar ? "activada" : "desactivada") + " correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/plantillas";
    }

    // Guarda el archivo físico en uploads/plantillas/ y devuelve la ruta pública /uploads/...
    private String guardarArchivo(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar el archivo de la plantilla.");
        }
        String original = archivo.getOriginalFilename() == null ? "plantilla" : archivo.getOriginalFilename();
        String extension = original.toLowerCase(Locale.ROOT);
        if (!extension.endsWith(".doc") && !extension.endsWith(".docx")
                && !extension.endsWith(".xls") && !extension.endsWith(".xlsx") && !extension.endsWith(".pdf")) {
            throw new IllegalArgumentException("Solo se admiten plantillas Word, Excel o PDF.");
        }

        Files.createDirectories(DIRECTORIO_PLANTILLAS);
        // Prefijo con timestamp: conserva las versiones anteriores del archivo en el disco
        String nombreFisico = System.currentTimeMillis() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path destino = DIRECTORIO_PLANTILLAS.resolve(nombreFisico);
        try (var in = archivo.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/plantillas/" + nombreFisico;
    }
}
