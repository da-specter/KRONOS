package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.AprendizFicha;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.Ficha;
import com.etapa_productiva.kronos.repository.AprendizFichaRepository;
import com.etapa_productiva.kronos.repository.EtapaProductivaRepository;
import com.etapa_productiva.kronos.repository.FichaRepository;
import com.etapa_productiva.kronos.repository.NotificacionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 📋 Módulo "Gestión de Fichas" del Gestor de Etapa: permite consultar todas las fichas
 * registradas junto con los aprendices matriculados en cada una.
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

    @GetMapping("/coordinador/fichas")
    public String verFichas(HttpSession session, Model model) {
        LoginResponse usuarioLogueado = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuarioLogueado == null) {
            return "redirect:/auth/login";
        }

        List<String> roles = usuarioLogueado.getRoles();
        if (roles == null || !roles.contains("GESTOR_ETAPA")) {
            return "redirect:/index";
        }

        model.addAttribute("usuario", usuarioLogueado);
        model.addAttribute("notificacionesNoLeidas",
                notificacionRepository.findByUsuarioDestinoIdUsuarioAndLeidoFalseOrderByFechaCreacionDesc(usuarioLogueado.getIdUsuario()));

        List<Ficha> fichas = fichaRepository.findAll();

        Map<Long, List<AprendizFicha>> aprendicesPorFicha = new HashMap<>();
        Map<Long, Set<Long>> aprendizFichaIdsEnEtapa = new HashMap<>();
        Map<Long, Integer> totalEnEtapaPorFicha = new HashMap<>();
        Map<Long, Integer> totalSinEtapaPorFicha = new HashMap<>();

        for (Ficha ficha : fichas) {
            List<AprendizFicha> aprendices = aprendizFichaRepository.findByFichaIdFicha(ficha.getIdFicha());
            aprendicesPorFicha.put(ficha.getIdFicha(), aprendices);

            List<EtapaProductiva> etapas = etapaProductivaRepository.findByAprendizFichaFichaIdFicha(ficha.getIdFicha());
            Set<Long> idsEnEtapa = new HashSet<>();
            for (EtapaProductiva etapa : etapas) {
                idsEnEtapa.add(etapa.getAprendizFicha().getIdAprendizFicha());
            }

            aprendizFichaIdsEnEtapa.put(ficha.getIdFicha(), idsEnEtapa);
            totalEnEtapaPorFicha.put(ficha.getIdFicha(), idsEnEtapa.size());
            totalSinEtapaPorFicha.put(ficha.getIdFicha(), aprendices.size() - idsEnEtapa.size());
        }

        model.addAttribute("fichas", fichas);
        model.addAttribute("aprendicesPorFicha", aprendicesPorFicha);
        model.addAttribute("aprendizFichaIdsEnEtapa", aprendizFichaIdsEnEtapa);
        model.addAttribute("totalEnEtapaPorFicha", totalEnEtapaPorFicha);
        model.addAttribute("totalSinEtapaPorFicha", totalSinEtapaPorFicha);

        return "gestion-fichas";
    }
}
