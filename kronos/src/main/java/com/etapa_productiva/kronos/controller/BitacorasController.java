package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.Bitacora;
import com.etapa_productiva.kronos.entity.CronogramaBitacoras;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.EvaluacionBitacora;
import com.etapa_productiva.kronos.entity.ResultadoEvaluacion;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.BitacoraRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.EvaluacionBitacoraRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import com.etapa_productiva.kronos.repository.SolicitudRepository;
import com.etapa_productiva.kronos.service.CronogramaService;
import com.etapa_productiva.kronos.service.EvaluacionFormatosService;
import com.etapa_productiva.kronos.service.SeguimientoEtapaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 📚 Módulo "Subir Bitácoras" del Aprendiz: dos secciones independientes,
 * Formato de Planeación y Bitácora, cada una con su propio asunto y archivo.
 */
@Controller
public class BitacorasController {

    @Autowired
    private EtapaProductivaRepository etapaProductivaRepository;

    @Autowired
    private BitacoraRepository bitacoraRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private SeguimientoEtapaService seguimientoEtapaService;

    @Autowired
    private AprendizFichaRepository aprendizFichaRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private EvaluacionBitacoraRepository evaluacionBitacoraRepository;

    @Autowired
    private EvaluacionFormatosService evaluacionFormatosService;

    @Autowired
    private CronogramaService cronogramaService;

    @GetMapping("/aprendiz/bitacoras")
    public String verBitacoras(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificaciones",
                notificacionRepository.findByUsuarioDestinoIdUsuarioOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario()).orElse(null);
        model.addAttribute("etapaActiva", etapaActiva);

        // Menú reactivo: refleja el mismo "📁 Formatos"/"Subir Bitácoras" condicional que usa /index
        // (sin duplicarlo en la sesión).
        SolicitudEtapaPractica solicitudActual = aprendizFichaRepository.findByUsuarioIdUsuario(usuarioLogueado.getIdUsuario())
                .map(AprendizFicha::getIdAprendizFicha)
                .flatMap(solicitudRepository::findByAprendizFichaIdAprendizFicha)
                .orElse(null);
        model.addAttribute("menuNavegacionActual",
                IndexController.menuAprendizReactivo(usuarioLogueado, solicitudActual, etapaActiva));

        if (etapaActiva != null) {
            model.addAttribute("momentos023", evaluacionFormatosService.obtenerMomentos(etapaActiva));
            model.addAttribute("formato023", evaluacionFormatosService.obtenerFormato023(etapaActiva.getIdEtapa()));

            List<Bitacora> bitacorasSubidas = bitacoraRepository.findByEtapaProductivaIdEtapaOrderByFechaEntregaDesc(etapaActiva.getIdEtapa());
            model.addAttribute("bitacorasSubidas", bitacorasSubidas);

            // Última evaluación de cada bitácora (si aún no hay ninguna, queda "en revisión")
            Map<Long, EvaluacionBitacora> ultimaEvaluacionPorBitacora = new HashMap<>();
            for (Bitacora bitacora : bitacorasSubidas) {
                evaluacionBitacoraRepository.findTopByBitacoraIdBitacoraOrderByFechaEvaluacionDesc(bitacora.getIdBitacora())
                        .ifPresent(evaluacion -> ultimaEvaluacionPorBitacora.put(bitacora.getIdBitacora(), evaluacion));
            }
            model.addAttribute("ultimaEvaluacionPorBitacora", ultimaEvaluacionPorBitacora);

            // Un cupo del cronograma vuelve a estar disponible para radicar si la última
            // bitácora subida para él fue evaluada como CORREGIR o REPROBADO.
            Map<Long, Bitacora> ultimaBitacoraPorCronograma = new HashMap<>();
            for (Bitacora bitacora : bitacorasSubidas) {
                Long idCronograma = bitacora.getCronogramaBitacora().getIdCronograma();
                Bitacora actual = ultimaBitacoraPorCronograma.get(idCronograma);
                if (actual == null || bitacora.getFechaHoraSubida().isAfter(actual.getFechaHoraSubida())) {
                    ultimaBitacoraPorCronograma.put(idCronograma, bitacora);
                }
            }
            Set<Long> cronogramasBloqueados = new HashSet<>();
            for (Map.Entry<Long, Bitacora> entrada : ultimaBitacoraPorCronograma.entrySet()) {
                EvaluacionBitacora evaluacion = ultimaEvaluacionPorBitacora.get(entrada.getValue().getIdBitacora());
                boolean puedeResubir = evaluacion != null
                        && (evaluacion.getResultado() == ResultadoEvaluacion.CORREGIR || evaluacion.getResultado() == ResultadoEvaluacion.REPROBADO);
                if (!puedeResubir) {
                    cronogramasBloqueados.add(entrada.getKey());
                }
            }

            List<CronogramaBitacoras> cronogramaPendiente = cronogramaService.obtenerOGenerarCronograma(etapaActiva)
                    .stream()
                    .filter(c -> !cronogramasBloqueados.contains(c.getIdCronograma()))
                    .collect(Collectors.toList());
            model.addAttribute("cronogramaPendiente", cronogramaPendiente);

            // ⏳ Para que la vista bloquee los cupos que aún no llegan a su fecha de apertura
            model.addAttribute("hoy", java.time.LocalDate.now());
        } else {
            model.addAttribute("momentos023", Collections.emptyList());
            model.addAttribute("formato023", null);
            model.addAttribute("bitacorasSubidas", Collections.emptyList());
            model.addAttribute("ultimaEvaluacionPorBitacora", Collections.emptyMap());
            model.addAttribute("cronogramaPendiente", Collections.emptyList());
            model.addAttribute("hoy", java.time.LocalDate.now());
        }

        return "bitacoras";
    }

    @PostMapping(value = "/aprendiz/bitacora", consumes = "multipart/form-data")
    public String subirBitacora(
            @RequestParam Long idCronograma,
            @RequestParam String asunto,
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }
        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("APRENDIZ")) {
            redirectAttributes.addFlashAttribute("error", "Solo un aprendiz puede subir bitácoras.");
            return "redirect:/index";
        }

        try {
            EtapaProductiva etapaActiva = etapaProductivaRepository.findByAprendizIdUsuario(usuarioLogueado.getIdUsuario())
                    .orElseThrow(() -> new IllegalStateException("No tienes una Etapa Productiva activa."));

            seguimientoEtapaService.subirBitacora(etapaActiva, idCronograma, asunto, archivo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/aprendiz/bitacoras";
    }
}
