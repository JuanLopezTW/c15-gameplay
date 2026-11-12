package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ChallengeState;
import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import co.eci.c15.gameplay.application.MatchResultService;
import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.NotInChallengeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChallengeWebTest {

    private HeroVillainChallengeService service;
    private MockMvc mvc;
    private MatchResultService results;
    private MapFreezeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        service = mock(HeroVillainChallengeService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ChallengeController(service)).build();
        results = mock(MatchResultService.class);
        interceptor = new MapFreezeInterceptor(service, results, new ObjectMapper());
    }

    private static String click(String userId) {
        return "{\"userId\":\"" + userId + "\"}";
    }

    @Test
    void acceptedClickAnswers204() throws Exception {
        mvc.perform(post("/api/partidas/m1/reto/clicks").principal(() -> "ana"))
                .andExpect(status().isNoContent());
        verify(service).click("m1", "ana");
    }

    @Test
    void clickWithoutChallengeAnswers409() throws Exception {
        doThrow(new ChallengeNotActiveException("m1")).when(service).click("m1", "ana");
        mvc.perform(post("/api/partidas/m1/reto/clicks").principal(() -> "ana"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void clickFromOutsiderAnswers403() throws Exception {
        doThrow(new NotInChallengeException("m1", "x")).when(service).click("m1", "x");
        mvc.perform(post("/api/partidas/m1/reto/clicks").principal(() -> "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void stateOfTheRunningChallenge() throws Exception {
        when(service.find("m1")).thenReturn(Optional.of(
                new ChallengeState("m1", ChallengeState.RUNNING, 12, Map.of("A", 3L, "B", -1L), null)));
        mvc.perform(get("/api/partidas/m1/reto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segundosRestantes").value(12))
                .andExpect(jsonPath("$.puntajes.A").value(3));
    }

    @Test
    void noChallengeAnswers404() throws Exception {
        when(service.find("m1")).thenReturn(Optional.empty());
        mvc.perform(get("/api/partidas/m1/reto")).andExpect(status().isNotFound());
    }

    @Test
    void frozenMapRejectsPostsWith409() throws Exception {
        when(service.isActive("m1")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/partidas/m1/jugadores/ana/movimiento");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString().contains("congelado"));
    }

    @Test
    void withoutChallengeTheMapWorks() throws Exception {
        when(service.isActive("m1")).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/partidas/m1/acertijos/a1/bloqueo");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void finishedMatchRejectsPostsWith409() throws Exception {
        when(results.isFinished("m1")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/partidas/m1/objetos/llave-1/recoleccion");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString().contains("termino"));
    }

    @Test
    void closingAPuzzleIsAllowedDuringTheChallenge() throws Exception {
        when(service.isActive("m1")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/partidas/m1/acertijos/a1/bloqueo");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
