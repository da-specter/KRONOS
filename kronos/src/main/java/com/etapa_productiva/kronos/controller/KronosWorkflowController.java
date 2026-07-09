package com.etapa_productiva.kronos.controller;

import com.etapa_productiva.kronos.dto.FinalizarEtapaDTO;
import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.EtapaProductiva;
import com.etapa_productiva.kronos.entity.ModalidadEtapa;
import com.etapa_productiva.kronos.service.KronosWorkflowService;
import com.etapa_productiva.kronos.entity.SolicitudEtapaPractica;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/workflow")
public class KronosWorkflowController {

    @Autowired
    private KronosWorkflowService workflowService;

    /**
     * Valida que haya una sesión activa (usuarioSesion) y que el usuario tenga
     * al menos uno de los roles permitidos para la acción solicitada.
     * Devuelve null si el acceso es válido, o la respuesta de error a retornar si no lo es.
     */
    private ResponseEntity<?> verificarAcceso(HttpSession session, String... rolesPermitidos) {
        LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Debes iniciar sesión para realizar esta acción.");
        }

        List<String> roles = usuario.getRoles();
        boolean autorizado = roles != null && Arrays.stream(rolesPermitidos).anyMatch(roles::contains);
        if (!autorizado) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permisos para realizar esta acción.");
        }

        return null;
    }

    /**
     * Traduce las excepciones del servicio a un código HTTP acorde:
     * - IllegalStateException / IllegalArgumentException: violación de una regla de negocio o dato inválido -> 400.
     * - Otra RuntimeException (p.ej. "Solicitud no encontrada"): el recurso referenciado no existe -> 404.
     * - Cualquier otra excepción: falla inesperada, no se expone el detalle interno -> 500.
     */
    private ResponseEntity<?> manejarExcepcion(Exception e) {
        if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        if (e instanceof RuntimeException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.internalServerError().body("Ocurrió un error inesperado. Intenta nuevamente más tarde.");
    }

    /**
     * 👨‍🎓 MOMENTO 1: El Aprendiz radica la solicitud inicial
     * POST http://localhost:8080/api/workflow/solicitud/crear
     */
    @PostMapping("/solicitud/crear")
    public ResponseEntity<?> crearSolicitud(
            @RequestParam Long idAprendizFicha,
            @RequestParam Long idSeccionFormato,
            @RequestParam ModalidadEtapa modalidad,
            HttpSession session) {
        ResponseEntity<?> denegado = verificarAcceso(session, "APRENDIZ");
        if (denegado != null) return denegado;

        try {
            SolicitudEtapaPractica solicitud = workflowService.aprendizCrearSolicitud(idAprendizFicha, idSeccionFormato, modalidad);
            return ResponseEntity.ok(solicitud);
        } catch (Exception e) {
            return manejarExcepcion(e);
        }
    }

    /**
     * 👨‍💼 MOMENTO 2: El Coordinador evalúa el primer filtro (Checks de fecha y competencias)
     * PUT http://localhost:8080/api/workflow/coordinador/evaluar-inicial/{idSolicitud}
     */
    @PutMapping("/coordinador/evaluar-inicial/{idSolicitud}")
    public ResponseEntity<?> evaluarPrimerFiltro(
            @PathVariable Long idSolicitud,
            @RequestParam boolean fechaOk,
            @RequestParam boolean competenciasOk,
            @RequestParam(required = false) String observacion,
            HttpSession session) {
        ResponseEntity<?> denegado = verificarAcceso(session, "GESTOR_ETAPA");
        if (denegado != null) return denegado;

        try {
            SolicitudEtapaPractica solicitud = workflowService.coordinadorEvaluarPrimerFiltro(idSolicitud, fechaOk, competenciasOk, observacion);
            return ResponseEntity.ok(solicitud);
        } catch (Exception e) {
            return manejarExcepcion(e);
        }
    }

    /**
     * 👨‍💼 El Gestor de Etapa califica los documentos/plantillas firmadas y los envía a Registro.
     * PUT http://localhost:8080/api/workflow/gestor/calificar-documentos/{idSolicitud}
     */
    @PutMapping("/gestor/calificar-documentos/{idSolicitud}")
    public ResponseEntity<?> calificarDocumentos(
            @PathVariable Long idSolicitud,
            @RequestParam boolean modalidadOk,
            @RequestParam boolean formatosOk,
            @RequestParam(required = false) String observacion,
            HttpSession session) {
        ResponseEntity<?> denegado = verificarAcceso(session, "GESTOR_ETAPA");
        if (denegado != null) return denegado;

        try {
            SolicitudEtapaPractica solicitud = workflowService.gestorCalificarDocumentos(idSolicitud, modalidadOk, formatosOk, observacion);
            return ResponseEntity.ok(solicitud);
        } catch (Exception e) {
            return manejarExcepcion(e);
        }
    }

    /**
     * 👨‍💼 El rol REGISTRO valida los documentos que el Gestor de Etapa ya calificó.
     * PUT http://localhost:8080/api/workflow/registro/validar-documentos/{idSolicitud}
     */
    @PutMapping("/registro/validar-documentos/{idSolicitud}")
    public ResponseEntity<?> validarDocumentosRegistro(
            @PathVariable Long idSolicitud,
            @RequestParam boolean aprobado,
            @RequestParam(required = false) String observacion,
            HttpSession session) {
        ResponseEntity<?> denegado = verificarAcceso(session, "REGISTRO");
        if (denegado != null) return denegado;

        try {
            SolicitudEtapaPractica solicitud = workflowService.registroValidarDocumentos(idSolicitud, aprobado, observacion);
            return ResponseEntity.ok(solicitud);
        } catch (Exception e) {
            return manejarExcepcion(e);
        }
    }

    /**
     * 🏢 MOMENTO FINAL: El rol REGISTRO digita la info de la empresa/contrato y activa la Etapa Práctica
     * POST http://localhost:8080/api/workflow/registro/registrar-etapa/{idSolicitud}
     */
    @PostMapping("/registro/registrar-etapa/{idSolicitud}")
    public ResponseEntity<?> registrarEtapa(
            @PathVariable Long idSolicitud,
            @Valid @RequestBody FinalizarEtapaDTO datosFinales, // Usamos un DTO para agrupar los objetos relacionales complejos
            HttpSession session) {
        ResponseEntity<?> denegado = verificarAcceso(session, "REGISTRO");
        if (denegado != null) return denegado;

        try {
            LoginResponse usuario = (LoginResponse) session.getAttribute("usuarioSesion");
            EtapaProductiva etapaActiva = workflowService.crearEtapaProductivaDesdeSolicitud(
                    idSolicitud,
                    datosFinales.getIdAprendizFicha(),
                    datosFinales.getIdEmpresa(),
                    datosFinales.getIdTipoContrato(),
                    datosFinales.getFechaInicio(),
                    datosFinales.getFechaFin(),
                    datosFinales.getNombreJefeInmediato(),
                    datosFinales.getCorreoJefeInmediato(),
                    datosFinales.getTelefonoJefeInmediato(),
                    usuario.getIdUsuario()
            );
            return ResponseEntity.ok(etapaActiva);
        } catch (Exception e) {
            return manejarExcepcion(e);
        }
    }
}