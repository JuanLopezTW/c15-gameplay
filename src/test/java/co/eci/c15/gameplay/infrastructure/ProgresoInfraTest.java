package co.eci.c15.gameplay.infrastructure;

import co.eci.c15.gameplay.application.CandadoEquipo;
import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.ObtenerMapaUseCase;
import co.eci.c15.gameplay.application.ProgresoPartidaDto;
import co.eci.c15.gameplay.application.ProgresoPartidaUseCase;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.infrastructure.events.ProgresoPartidaListener;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.ProgresoRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.web.ProgresoController;
import co.eci.c15.gameplay.infrastructure.web.StompProgresoNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProgresoInfraTest {

    private List<ProgresoPartidaDto> avisos;
    private ProgresoPartidaListener listener;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        avisos = new ArrayList<>();
        ProgresoPartidaUseCase useCase = new ProgresoPartidaUseCase(new ProgresoRepositoryEnMemoria(),
                new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), new GeneradorMapa()), avisos::add, CandadoEquipo.ninguno());
        listener = new ProgresoPartidaListener(useCase);
        mvc = MockMvcBuilders.standaloneSetup(new ProgresoController(useCase)).build();
    }

    @Test
    void elEventoDeInicioInicializaElProgresoYElDeResueltoLoActualiza() {
        listener.onPartidaIniciada(new PartidaIniciadaEvent("p1", Map.of("e1", List.of("a1"), "e2", List.of("b1"))));
        listener.onAcertijoResuelto(new AcertijoResueltoEvent("p1", "e2", "acertijo-1", "b1"));

        assertEquals(2, avisos.size());
        ProgresoPartidaDto ultimo = avisos.get(1);
        assertEquals(0, ultimo.equipos().get(0).resueltos());
        assertEquals(1, ultimo.equipos().get(1).resueltos());
        assertEquals("e2", ultimo.equipos().get(1).equipoId());
    }

    @Test
    void stompPublicaEnElTopicDeProgresoDeLaPartida() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        ProgresoPartidaDto dto = new ProgresoPartidaDto("p1", List.of());

        new StompProgresoNotifier(messaging).notificar(dto);

        verify(messaging).convertAndSend("/topic/partidas/p1/progreso", dto);
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void getDevuelveElProgresoDeTodosLosEquipos() throws Exception {
        listener.onPartidaIniciada(new PartidaIniciadaEvent("p1", Map.of("e1", List.of("a1"), "e2", List.of("b1"))));
        listener.onAcertijoResuelto(new AcertijoResueltoEvent("p1", "e1", "acertijo-1", "a1"));

        mvc.perform(get("/api/partidas/p1/progreso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value("p1"))
                .andExpect(jsonPath("$.equipos", hasSize(2)))
                .andExpect(jsonPath("$.equipos[0].equipoId").value("e1"))
                .andExpect(jsonPath("$.equipos[0].resueltos").value(1))
                .andExpect(jsonPath("$.equipos[0].total").value(3))
                .andExpect(jsonPath("$.equipos[0].acertijosResueltos[0]").value("acertijo-1"))
                .andExpect(jsonPath("$.equipos[1].resueltos").value(0));
    }

    @Test
    void getDeUnaPartidaSinProgresoDevuelve404() throws Exception {
        mvc.perform(get("/api/partidas/desconocida/progreso"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
