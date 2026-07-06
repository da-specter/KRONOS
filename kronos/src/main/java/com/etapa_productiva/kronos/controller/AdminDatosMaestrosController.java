package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.dto.ResultadoImportacionAdmin;
import com.etapa_productiva.kronos.entity.Jornada;
import com.etapa_productiva.kronos.repository.*;
import com.etapa_productiva.kronos.service.AdminDatosMaestrosService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🗄️ Módulo "Datos Maestros" del Administrador:
 * Áreas de Formación, Programas de Formación, Gestión de Fichas y División Territorial (DIVIPOLA).
 * Todas las rutas viven bajo /admin/** y exigen el rol ADMINISTRADOR en la sesión.
 */
@Controller
@RequestMapping("/admin")
public class AdminDatosMaestrosController {

    @Autowired private AdminDatosMaestrosService datosMaestrosService;
    @Autowired private AreasFormacionRepository areasFormacionRepository;
    @Autowired private ProgramasFormacionRepository programasFormacionRepository;
    @Autowired private NivelFormacionRepository nivelFormacionRepository;
    @Autowired private FichaRepository fichaRepository;
    @Autowired private DepartamentoRepository departamentoRepository;
    @Autowired private MunicipioRepository municipioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // ══════════════════════════ ÁREAS DE FORMACIÓN ══════════════════════════

    @GetMapping("/areas")
    public String verAreas(HttpSession session, Model model) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        model.addAttribute("usuario", admin);
        model.addAttribute("areas", areasFormacionRepository.findAllByOrderByNombreAreaFormacionAsc());
        model.addAttribute("usuariosActivos", usuarioRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEstado())).toList());
        return "admin-areas";
    }

    @PostMapping("/areas/crear")
    public String crearArea(@RequestParam String nombreArea,
                            @RequestParam Long idUsuarioCoordinador,
                            HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.crearArea(nombreArea, idUsuarioCoordinador, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Área de formación creada correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/areas";
    }

    @PostMapping("/areas/editar")
    public String editarArea(@RequestParam Long idArea,
                             @RequestParam(required = false) String nombreArea,
                             @RequestParam(required = false) Long idUsuarioCoordinador,
                             @RequestParam(required = false) Boolean estado,
                             HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.editarArea(idArea, nombreArea, idUsuarioCoordinador, estado, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Área de formación actualizada correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/areas";
    }

    @PostMapping(value = "/areas/importar", consumes = "multipart/form-data")
    public String importarAreas(@RequestParam("archivo") MultipartFile archivo,
                                @RequestParam(required = false) Long idUsuarioCoordinador,
                                HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            ResultadoImportacionAdmin resultado = datosMaestrosService.importarAreas(archivo, idUsuarioCoordinador, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Importación de áreas finalizada. " + resultado.resumen());
            if (!resultado.errores().isEmpty()) {
                redirect.addFlashAttribute("erroresImportacion", resultado.errores());
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/areas";
    }

    // ══════════════════════════ PROGRAMAS DE FORMACIÓN ══════════════════════════

    @GetMapping("/programas")
    public String verProgramas(HttpSession session, Model model) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        model.addAttribute("usuario", admin);
        model.addAttribute("programas", programasFormacionRepository.findAll());
        model.addAttribute("areas", areasFormacionRepository.findAllByOrderByNombreAreaFormacionAsc());
        model.addAttribute("niveles", nivelFormacionRepository.findAll());
        return "admin-programas";
    }

    @PostMapping("/programas/crear")
    public String crearPrograma(@RequestParam String nombrePrograma,
                                @RequestParam Long idArea,
                                @RequestParam Long idNivel,
                                HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.crearPrograma(nombrePrograma, idArea, idNivel, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Programa de formación creado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/programas";
    }

    @PostMapping("/programas/editar")
    public String editarPrograma(@RequestParam Long idPrograma,
                                 @RequestParam(required = false) String nombrePrograma,
                                 @RequestParam(required = false) Long idArea,
                                 @RequestParam(required = false) Long idNivel,
                                 @RequestParam(required = false) Boolean estado,
                                 HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.editarPrograma(idPrograma, nombrePrograma, idArea, idNivel, estado, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Programa de formación actualizado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/programas";
    }

    // ══════════════════════════ GESTIÓN DE FICHAS ══════════════════════════

    @GetMapping("/fichas")
    public String verFichas(HttpSession session, Model model) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        model.addAttribute("usuario", admin);
        model.addAttribute("fichas", fichaRepository.findAll());
        model.addAttribute("programas", programasFormacionRepository.findAll());
        model.addAttribute("jornadas", Jornada.values());
        return "admin-fichas";
    }

    @PostMapping("/fichas/crear")
    public String crearFicha(@RequestParam String numeroFicha,
                             @RequestParam Long idPrograma,
                             @RequestParam(required = false) Jornada jornada,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                             HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.crearFicha(numeroFicha, idPrograma, jornada, fechaInicio, fechaFin, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Ficha creada correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/fichas";
    }

    @PostMapping("/fichas/editar")
    public String editarFicha(@RequestParam Long idFicha,
                              @RequestParam(required = false) Long idPrograma,
                              @RequestParam(required = false) Jornada jornada,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                              @RequestParam(required = false) Boolean estado,
                              HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.editarFicha(idFicha, idPrograma, jornada, fechaInicio, fechaFin, estado, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Ficha actualizada correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/fichas";
    }

    @PostMapping(value = "/fichas/importar", consumes = "multipart/form-data")
    public String importarFichas(@RequestParam("archivo") MultipartFile archivo,
                                 @RequestParam(required = false) Long idPrograma,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                                 HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            ResultadoImportacionAdmin resultado = datosMaestrosService.importarFichas(
                    archivo, idPrograma, fechaInicio, fechaFin, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Carga masiva de fichas finalizada. " + resultado.resumen());
            if (!resultado.errores().isEmpty()) {
                redirect.addFlashAttribute("erroresImportacion", resultado.errores());
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/fichas";
    }

    // ══════════════════════════ DIVISIÓN TERRITORIAL (DIVIPOLA) ══════════════════════════

    @GetMapping("/divipola")
    public String verDivipola(HttpSession session, Model model) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";

        var departamentos = departamentoRepository.findAll();
        // Municipios agrupados por departamento para pintar el árbol de consulta
        Map<Long, List<com.etapa_productiva.kronos.entity.Municipio>> municipiosPorDepartamento = new HashMap<>();
        for (var departamento : departamentos) {
            municipiosPorDepartamento.put(departamento.getIdDepartamento(),
                    municipioRepository.findByDepartamentoIdDepartamento(departamento.getIdDepartamento()));
        }

        model.addAttribute("usuario", admin);
        model.addAttribute("departamentos", departamentos);
        model.addAttribute("municipiosPorDepartamento", municipiosPorDepartamento);
        model.addAttribute("totalMunicipios", municipioRepository.count());
        return "admin-divipola";
    }

    @PostMapping("/divipola/departamento")
    public String crearDepartamento(@RequestParam String nombreDepartamento,
                                    HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.crearDepartamento(nombreDepartamento, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Departamento creado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/divipola";
    }

    @PostMapping("/divipola/municipio")
    public String crearMunicipio(@RequestParam String nombreMunicipio,
                                 @RequestParam Long idDepartamento,
                                 HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.crearMunicipio(nombreMunicipio, idDepartamento, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Municipio creado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/divipola";
    }

    @PostMapping("/divipola/departamento/renombrar")
    public String renombrarDepartamento(@RequestParam Long idDepartamento,
                                        @RequestParam String nombreDepartamento,
                                        HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.actualizarNombreDepartamento(idDepartamento, nombreDepartamento, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Departamento actualizado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/divipola";
    }

    @PostMapping("/divipola/municipio/renombrar")
    public String renombrarMunicipio(@RequestParam Long idMunicipio,
                                     @RequestParam String nombreMunicipio,
                                     HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            datosMaestrosService.actualizarNombreMunicipio(idMunicipio, nombreMunicipio, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Municipio actualizado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/divipola";
    }

    @PostMapping(value = "/divipola/importar", consumes = "multipart/form-data")
    public String importarDivipola(@RequestParam("archivo") MultipartFile archivo,
                                   HttpSession session, RedirectAttributes redirect) {
        LoginResponse admin = validarAdmin(session);
        if (admin == null) return "redirect:/auth/login";
        try {
            ResultadoImportacionAdmin resultado = datosMaestrosService.importarDivipola(archivo, admin.getIdUsuario());
            redirect.addFlashAttribute("exito", "Carga masiva DIVIPOLA finalizada. " + resultado.resumen());
            if (!resultado.errores().isEmpty()) {
                redirect.addFlashAttribute("erroresImportacion", resultado.errores());
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/divipola";
    }

    // 🔐 Solo el rol ADMINISTRADOR puede entrar a los módulos /admin/**
    static LoginResponse validarAdmin(HttpSession session) {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ADMINISTRADOR")) {
            return null;
        }
        return usuario;
    }
}
