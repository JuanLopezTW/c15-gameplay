package co.eci.c15;

import co.eci.c15.plataforma.eventos.SeguimientoEventos;
import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import co.eci.c15.gameplay.domain.ChallengeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Reto Heroe vs Verdugo (HU-44.5) contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class RetoHeroeVerdugoFlujoTest {

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventosPartida;

    private static final int CLICKS_PER_PLAYER = 60;

    @Autowired
    private SeguimientoEventos entreServicios;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private HeroVillainChallengeService challenges;

    @Test
    void retoCompletoConClicksConcurrentesYMapaCongelado() throws Exception {
        Map<String, String> teamOf = new HashMap<>();
        String matchId = partidaIniciada(teamOf);

        mvc.perform(post("/dev/partidas/{id}/reto/inicio", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));
        mvc.perform(post("/dev/partidas/{id}/reto/inicio", matchId))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", matchId, "ana")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"x\":1,\"y\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("congelado")));

        Map<String, String> roleOf = new HashMap<>();
        for (String team : new HashMap<>(teamOf).values().stream().distinct().toList()) {
            JsonNode roles = leer(mvc.perform(get("/api/partidas/{m}/equipos/{e}/roles", matchId, team))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("roles");
            roles.fields().forEachRemaining(e -> roleOf.put(e.getKey(), e.getValue().asText()));
        }
        assertEquals(4, roleOf.size());

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (String player : roleOf.keySet()) {
            for (int t = 0; t < 2; t++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    for (int i = 0; i < CLICKS_PER_PLAYER / 2; i++) {
                        mvc.perform(post("/api/partidas/{m}/reto/clicks", matchId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"userId\":\"" + player + "\"}"))
                                .andExpect(status().isNoContent());
                    }
                    return null;
                }));
            }
        }
        go.countDown();
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        Map<String, Long> expected = new HashMap<>();
        teamOf.values().forEach(team -> expected.put(team, 0L));
        roleOf.forEach((player, role) -> {
            String team = teamOf.get(player);
            if (role.equals("HEROE")) expected.merge(team, (long) CLICKS_PER_PLAYER, Long::sum);
            else expected.keySet().stream().filter(t -> !t.equals(team))
                    .forEach(rival -> expected.merge(rival, (long) -CLICKS_PER_PLAYER, Long::sum));
        });

        ChallengeResult result = challenges.finish(matchId).orElseThrow();
        assertEquals(expected, result.scores());

        mvc.perform(post("/api/partidas/{m}/reto/clicks", matchId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"ana\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", matchId, "ana")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"x\":1,\"y\":1}"))
                .andExpect(content().string(not(containsString("congelado"))));
    }

    @Test
    void sinPartidaNoHayReto() throws Exception {
        mvc.perform(post("/dev/partidas/{id}/reto/inicio", "no-existe"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/partidas/{m}/reto", "no-existe"))
                .andExpect(status().isNotFound());
    }

    /** Lo que publica salas al arrancar: dos equipos (ana y beto, caro y dani) y 10 minutos de partida. */
    private String partidaIniciada(Map<String, String> teamOf) {
        String matchId = "m-" + java.util.UUID.randomUUID();
        String equipo1 = matchId + "-e1";
        String equipo2 = matchId + "-e2";
        teamOf.put("ana", equipo1);
        teamOf.put("beto", equipo1);
        teamOf.put("caro", equipo2);
        teamOf.put("dani", equipo2);
        eventosPartida.publishEvent(new co.eci.c15.common.events.PartidaIniciadaEvent(matchId,
                Map.of(equipo1, java.util.List.of("ana", "beto"), equipo2, java.util.List.of("caro", "dani")),
                Duration.ofMinutes(10)));
        return matchId;
    }

    private JsonNode leer(String body) throws Exception {
        return json.readTree(body);
    }
}
