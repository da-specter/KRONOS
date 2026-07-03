package com.etapa_productiva.kronos;

import com.etapa_productiva.kronos.dto.LoginResponse;
import com.etapa_productiva.kronos.entity.*;
import com.etapa_productiva.kronos.repository.*;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IndexTemplateRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private DocumentoSolicitudRepository documentoSolicitudRepository;
    @MockitoBean
    private InstructorSeguimientoRepository instructorSeguimientoRepository;
    @MockitoBean
    private InstructorSeguimientoFichaRepository instructorSeguimientoFichaRepository;
    @MockitoBean
    private InstructorTecnicoRepository instructorTecnicoRepository;
    @MockitoBean
    private InstructorTecnicoFichaRepository instructorTecnicoFichaRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario usuarioMock(String nombre, String apellido) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        return u;
    }

    private Ficha fichaMock() {
        ProgramasFormacion programa = ProgramasFormacion.builder().nombrePrograma("ADSO").build();
        return Ficha.builder()
                .numeroFicha("2555228")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusMonths(6))
                .programaFormacion(programa)
                .build();
    }

    @Test
    void rendersDashboardForEachRoleWithoutTemplateErrors() throws Exception {
        AprendizFicha aprendizFichaSolicitante = AprendizFicha.builder()
                .idAprendizFicha(10L)
                .usuario(usuarioMock("Sofia", "Ramirez"))
                .ficha(fichaMock())
                .build();
        SolicitudEtapaPractica solicitudMock = SolicitudEtapaPractica.builder()
                .idSolicitud(1L).aprendizFicha(aprendizFichaSolicitante)
                .modalidadSolicitada(ModalidadEtapa.PRESENCIAL).build();

        DocumentoSolicitud documento = DocumentoSolicitud.builder()
                .solicitud(solicitudMock)
                .plantillaFormato(PlantillaFormato.builder().nombreDocumento("Contrato de Aprendizaje").build())
                .fechaSubida(LocalDateTime.now())
                .estadoValidacion(EstadoValidacion.PENDIENTE)
                .build();

        when(solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE_REVISION))
                .thenReturn(List.of(solicitudMock));
        when(documentoSolicitudRepository.findByEstadoValidacion(EstadoValidacion.PENDIENTE))
                .thenReturn(List.of(documento));

        InstructorSeguimiento instSeg = InstructorSeguimiento.builder().idInstructorSeguimiento(5L).build();
        when(instructorSeguimientoRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(instSeg));
        when(instructorSeguimientoFichaRepository.findByInstructorSeguimientoIdInstructorSeguimientoAndEstadoTrue(5L))
                .thenReturn(List.of(InstructorSeguimientoFicha.builder().ficha(fichaMock()).build()));

        InstructorTecnico instTec = InstructorTecnico.builder().idInstructorTecnico(7L).build();
        when(instructorTecnicoRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(instTec));
        when(instructorTecnicoFichaRepository.findByInstructorTecnicoIdInstructorTecnicoAndEstadoTrue(7L))
                .thenReturn(List.of(InstructorTecnicoFicha.builder().ficha(fichaMock()).build()));

        when(usuarioRepository.count()).thenReturn(42L);
        when(usuarioRepository.countByEstado(true)).thenReturn(40L);
        when(usuarioRepository.countByEstado(false)).thenReturn(2L);

        LoginResponse usuario = LoginResponse.builder()
                .idUsuario(1L)
                .nombre("Ana")
                .apellido("Gomez")
                .correo("ana@test.com")
                .roles(List.of("GESTOR_ETAPA", "INSTRUCTOR_SEGUIMIENTO",
                        "INSTRUCTOR_TECNICO", "ADMINISTRADOR"))
                .menuNavegacion(List.of())
                .build();

        mockMvc.perform(get("/index").sessionAttr("usuarioSesion", usuario))
                .andExpect(status().isOk());
    }
}
