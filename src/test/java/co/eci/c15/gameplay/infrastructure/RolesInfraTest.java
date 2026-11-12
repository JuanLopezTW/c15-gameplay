package co.eci.c15.gameplay.infrastructure;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.AsignacionRolesDto;
import co.eci.c15.gameplay.application.AsignarRolesUseCase;
import co.eci.c15.gameplay.application.ConsultarRolesUseCase;
import co.eci.c15.gameplay.domain.AsignadorRoles;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.infrastructure.events.RolesPartidaListener;
import co.eci.c15.gameplay.infrastructure.persistence.RolesRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.web.RolesController;
import co.eci.c15.gameplay.infrastructure.web.StompRolesNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RolesInfraTest {

    private RolesRepositoryEnMemoria repo;
    private AsignarRolesUseCase asignar;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repo = new RolesRepositoryEnMemoria();
        asignar = new AsignarRolesUseCase(new AsignadorRoles(new Random(5)), repo, dto -> { });
        mvc = MockMvcBuilders.standaloneSetup(new RolesController(asignar, new ConsultarRolesUseCase(repo))).build();
    }

    @Test
    void elEventoDeInicioDePartidaAsignaLosRoles() {
        new RolesPartidaListener(asignar).onPartidaIniciada(
                new PartidaIniciadaEvent("p1", Map.of("e1", List.of("a1", "a2", "a3"))));

        assertTrue(repo.findByEquipo("p1", "e1").isPresent());
        assertTrue(repo.findRol("p1", "a1").isPresent());
    }

    @Test
    void stompPublicaEnElTopicDeRolesDelEquipo() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        AsignacionRolesDto dto = new AsignacionRolesDto("p1", "e1", new TreeMap<>(Map.of("a1", Rol.HEROE)));

        new StompRolesNotifier(messaging).notificar(dto);

        verify(messaging, times(1)).convertAndSend("/topic/partidas/p1/equipos/e1/roles", dto);
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void postAsignaYGetConsultaElRol() throws Exception {
        mvc.perform(post("/api/partidas/p1/roles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipos\":{\"e1\":[\"a1\",\"a2\",\"a3\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipoId").value("e1"))
                .andExpect(jsonPath("$[0].roles.a1").exists());

        String rol = repo.findRol("p1", "a2").orElseThrow().name();
        mvc.perform(get("/api/partidas/p1/jugadores/a2/rol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value(rol));
        mvc.perform(get("/api/partidas/p1/equipos/e1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.a3").exists());
    }

    @Test
    void consultasSinAsignacionDevuelven404() throws Exception {
        mvc.perform(get("/api/partidas/p1/jugadores/a1/rol")).andExpect(status().isNotFound());
        mvc.perform(get("/api/partidas/p1/equipos/e1/roles")).andExpect(status().isNotFound());
    }

    @Test
    void asignarSinEquiposDevuelve400() throws Exception {
        mvc.perform(post("/api/partidas/p1/roles").contentType(MediaType.APPLICATION_JSON).content("{\"equipos\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        assertEquals(java.util.Optional.empty(), repo.findByEquipo("p1", "e1"));
    }
}
