package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.entity.FormatoReporte;
import com.etapa_productiva.kronos.entity.Usuario;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.FichaRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.UsuarioRepository;
import com.etapa_productiva.kronos.service.AuthService;
import com.etapa_productiva.kronos.service.GestionFichasService;
import com.etapa_productiva.kronos.service.InstructorTecnicoService;
import com.etapa_productiva.kronos.service.ReporteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 📋 Módulo "Gestión de Fichas": consulta de todas las fichas registradas junto con los
 * aprendices matriculados en cada una y su situación (en etapa, sin etapa, por certificar,
 * certificados). El Gestor de Etapa tiene además la plantilla e importación de Excel; Registro
 * y Administrador acceden en modo solo lectura + exportación (ver {@code soloLectura}).
 */
@Controller
public class FichaController {

    @Autowired
    private FichaRepository fichaRepository;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private GestionFichasService gestionFichasService;

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/coordinador/fichas")
    public String verFichas(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        boolean autorizado = roles != null && (roles.contains("GESTOR_ETAPA")
                || roles.contains("REGISTRO") || roles.contains("ADMINISTRADOR"));
        if (!autorizado) {
            return "redirect:/index";
        }
        // Solo el Gestor de Etapa importa/edita; Registro y Administrador consultan y exportan.
        boolean soloLectura = !roles.contains("GESTOR_ETAPA");

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        List<Ficha> fichas = fichaRepository.findAll();

        Map<Long, List<AprendizFicha>> aprendicesPorFicha = new HashMap<>();
        Map<Long, Set<Long>> aprendizFichaIdsEnEtapa = new HashMap<>();
        Map<Long, Integer> totalEnEtapaPorFicha = new HashMap<>();
        Map<Long, Integer> totalSinEtapaPorFicha = new HashMap<>();
        Map<Long, Integer> totalPorCertificarPorFicha = new HashMap<>();
        Map<Long, Integer> totalCertificadosPorFicha = new HashMap<>();

        for (Ficha ficha : fichas) {
            List<AprendizFicha> aprendices = aprendizFichaRepository.findByFichaIdFicha(ficha.getIdFicha());
            aprendicesPorFicha.put(ficha.getIdFicha(), aprendices);

            List<EtapaProductiva> etapas = etapaProductivaRepository.findByAprendizFichaFichaIdFicha(ficha.getIdFicha());
            Set<Long> idsEnEtapa = new HashSet<>();
            Map<Long, EtapaProductiva> etapaPorAprendizFicha = new HashMap<>();
            for (EtapaProductiva etapa : etapas) {
                Long idAprendizFicha = etapa.getAprendizFicha().getIdAprendizFicha();
                idsEnEtapa.add(idAprendizFicha);
                etapaPorAprendizFicha.put(idAprendizFicha, etapa);
            }
            aprendizFichaIdsEnEtapa.put(ficha.getIdFicha(), idsEnEtapa);

            // Situación por aprendiz (certificado > por certificar > en etapa > sin etapa),
            // mismo criterio que usa el dashboard del Instructor Técnico, ahora agregado por ficha.
            int enEtapa = 0, sinEtapa = 0, porCertificar = 0, certificados = 0;
            for (AprendizFicha matricula : aprendices) {
                String situacion = InstructorTecnicoService.clasificarSituacion(
                        matricula, etapaPorAprendizFicha.get(matricula.getIdAprendizFicha()));
                if (situacion.equals(InstructorTecnicoService.SIT_CERTIFICADO)) certificados++;
                else if (situacion.equals(InstructorTecnicoService.SIT_POR_CERTIFICAR)) porCertificar++;
                else if (situacion.equals(InstructorTecnicoService.SIT_EN_ETAPA)) enEtapa++;
                else sinEtapa++;
            }
            totalEnEtapaPorFicha.put(ficha.getIdFicha(), enEtapa);
            totalSinEtapaPorFicha.put(ficha.getIdFicha(), sinEtapa);
            totalPorCertificarPorFicha.put(ficha.getIdFicha(), porCertificar);
            totalCertificadosPorFicha.put(ficha.getIdFicha(), certificados);
        }

        model.addAttribute("fichas", fichas);
        model.addAttribute("aprendicesPorFicha", aprendicesPorFicha);
        model.addAttribute("aprendizFichaIdsEnEtapa", aprendizFichaIdsEnEtapa);
        model.addAttribute("totalEnEtapaPorFicha", totalEnEtapaPorFicha);
        model.addAttribute("totalSinEtapaPorFicha", totalSinEtapaPorFicha);
        model.addAttribute("totalPorCertificarPorFicha", totalPorCertificarPorFicha);
        model.addAttribute("totalCertificadosPorFicha", totalCertificadosPorFicha);
        model.addAttribute("soloLectura", soloLectura);

        return "gestion-fichas";
    }

    /**
     * 📥 Exporta el listado de fichas (Excel o PDF), previa re-autenticación del Gestor.
     * Registra el archivo en el historial de reportes (FICHAS_EXCEL o EJECUTIVO).
     * POST /coordinador/fichas/exportar
     */
    @PostMapping("/coordinador/fichas/exportar")
    @ResponseBody
    public ResponseEntity<?> exportarFichas(
            @RequestParam String formato,
            @RequestParam String contrasena,
            HttpSession session) throws Exception {

        ResponseEntity<?> rechazo = validarAccesoYContrasena(session, contrasena);
        if (rechazo != null) return rechazo;
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = gestionFichasService.exportarFichasPdf();
            reporteService.registrar(usuario.getIdUsuario(), "FICHAS", FormatoReporte.PDF, pdf);
            return descarga(pdf, MediaType.APPLICATION_PDF, "fichas_" + fecha + ".pdf");
        }
        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = gestionFichasService.exportarFichasExcel();
            reporteService.registrar(usuario.getIdUsuario(), "FICHAS", FormatoReporte.XLSX, excel);
            return descarga(excel, MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), "fichas_" + fecha + ".xlsx");
        }
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }

    /**
     * 📥 Exporta los aprendices de una ficha puntual (botón "Ver aprendices" → Exportar),
     * previa re-autenticación. Registra el reporte como APRENDIZ_EXCEL o EJECUTIVO.
     * POST /coordinador/fichas/aprendices/exportar
     */
    @PostMapping("/coordinador/fichas/aprendices/exportar")
    @ResponseBody
    public ResponseEntity<?> exportarAprendicesDeFicha(
            @RequestParam Long idFicha,
            @RequestParam String formato,
            @RequestParam String contrasena,
            HttpSession session) throws Exception {

        ResponseEntity<?> rechazo = validarAccesoYContrasena(session, contrasena);
        if (rechazo != null) return rechazo;
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");

        Ficha ficha = gestionFichasService.buscarFicha(idFicha);
        String base = "aprendices_ficha_" + ficha.getNumeroFicha();
        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = gestionFichasService.exportarAprendicesFichaPdf(ficha);
            reporteService.registrar(usuario.getIdUsuario(), "APRENDIZ_FICHA", FormatoReporte.PDF, pdf);
            return descarga(pdf, MediaType.APPLICATION_PDF, base + ".pdf");
        }
        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = gestionFichasService.exportarAprendicesFichaExcel(ficha);
            reporteService.registrar(usuario.getIdUsuario(), "APRENDIZ_FICHA", FormatoReporte.XLSX, excel);
            return descarga(excel, MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), base + ".xlsx");
        }
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de exportación no soportado."));
    }

    /**
     * 📋 Descarga la plantilla Excel en blanco para diligenciar la importación de
     * aprendices+ficha. GET /coordinador/fichas/plantilla
     */
    @GetMapping("/coordinador/fichas/plantilla")
    public ResponseEntity<byte[]> descargarPlantillaImportacion(HttpSession session) throws IOException {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/auth/login").build();
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("GESTOR_ETAPA")) {
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/index").build();
        }

        byte[] excel = gestionFichasService.generarPlantillaImportacion();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("Content-Disposition", "attachment; filename=plantilla_importacion_aprendices_ficha.xlsx")
                .body(excel);
    }

    /**
     * 📤 Importa un Excel de aprendices+ficha, creando/actualizando Ficha, Programa, Usuario y matrícula.
     * POST /coordinador/fichas/importar (multipart/form-data)
     */
    @PostMapping(value = "/coordinador/fichas/importar", consumes = "multipart/form-data")
    public String importarFichas(
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return "redirect:/auth/login";
        }
        if (usuario.getRoles() == null || !usuario.getRoles().contains("GESTOR_ETAPA")) {
            return "redirect:/index";
        }

        try {
            GestionFichasService.ResultadoImportacion r = gestionFichasService.importar(archivo);
            StringBuilder resumen = new StringBuilder();
            resumen.append("Importación completada: ").append(r.filas()).append(" filas procesadas. ")
                    .append("Fichas nuevas: ").append(r.fichasCreadas()).append(", ")
                    .append("programas nuevos: ").append(r.programasCreados()).append(", ")
                    .append("aprendices nuevos: ").append(r.aprendicesCreados()).append(", ")
                    .append("matrículas nuevas: ").append(r.matriculasCreadas()).append(".");
            if (!r.errores().isEmpty()) {
                resumen.append(" ⚠️ ").append(r.errores().size()).append(" fila(s) con error: ")
                        .append(String.join(" | ", r.errores().subList(0, Math.min(5, r.errores().size()))));
            }
            redirectAttributes.addFlashAttribute("exito", resumen.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/coordinador/fichas";
    }

    // ─────────────────────────── Helpers ───────────────────────────

    // Verifica sesión, rol autorizado (Gestor de Etapa, Registro o Administrador) y que la
    // contraseña coincida con la del usuario en sesión. Devuelve null si todo es válido.
    private ResponseEntity<?> validarAccesoYContrasena(HttpSession session, String contrasena) {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."));
        }
        List<String> roles = usuario.getRoles();
        boolean autorizado = roles != null && (roles.contains("GESTOR_ETAPA")
                || roles.contains("REGISTRO") || roles.contains("ADMINISTRADOR"));
        if (!autorizado) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "No tienes permiso para exportar esta información."));
        }
        Usuario entidad = usuarioRepository.findById(usuario.getIdUsuario()).orElse(null);
        if (!authService.verificarContrasena(entidad, contrasena)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Contraseña incorrecta. Verifícala e inténtalo de nuevo."));
        }
        return null;
    }

    private ResponseEntity<byte[]> descarga(byte[] contenido, MediaType tipo, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header("Content-Disposition", "attachment; filename=" + nombreArchivo)
                .body(contenido);
    }
}
