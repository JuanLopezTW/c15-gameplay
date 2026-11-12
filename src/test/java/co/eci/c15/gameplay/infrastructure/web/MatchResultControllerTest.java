package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.MatchResultService;
import co.eci.c15.gameplay.domain.MatchResult;
import co.eci.c15.gameplay.domain.TeamScore;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchResultControllerTest {

    private final MatchResultService service = mock(MatchResultService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MatchResultController(service)).build();

    @Test
    void finishedMatchReturnsTheResult() throws Exception {
        when(service.find("m1")).thenReturn(Optional.of(new MatchResult("m1", MatchResult.Reason.TIME_UP, "A",
                List.of(), List.of(new TeamScore("A", 2, 1, 3), new TeamScore("B", 1, 0, 1)))));

        mvc.perform(get("/api/partidas/m1/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipoGanadorId").value("A"))
                .andExpect(jsonPath("$.motivo").value("TIEMPO_AGOTADO"))
                .andExpect(jsonPath("$.empate").value(false))
                .andExpect(jsonPath("$.clasificacion[0].puntaje").value(3));
    }

    @Test
    void runningMatchAnswers404() throws Exception {
        when(service.find("m1")).thenReturn(Optional.empty());
        mvc.perform(get("/api/partidas/m1/resultado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
