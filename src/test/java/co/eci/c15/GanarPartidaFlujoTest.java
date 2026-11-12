package co.eci.c15;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.plataforma.eventos.SeguimientoEventos;
import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.events.TimeUpEvent;
import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fin de la partida y ganador (HU-67.6) contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class GanarPartidaFlujoTest {

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventosPartida;

    @Autowired
    private FinesDePartida fines;

    @Autowired
    private SeguimientoEventos entreServicios;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private HeroVillainChallengeService challenges;

    @Test
    void resolverTodosLosAcertijosTerminaLaPartidaYGanaElDeMasPuntaje() throws Exception {
        Map<String, String> teamOf = new LinkedHashMap<>();
        String matchId = partidaIniciada("Sala ganar", teamOf);
        String equipoA = teamOf.get("ana");
        String equipoB = teamOf.get("caro");

        mvc.perform(get("/api/partidas/{m}/resultado", matchId)).andExpect(status().isNotFound());

        // El equipo B gana el reto: solo hace click su heroe
        mvc.perform(post("/dev/partidas/{m}/reto/inicio", matchId)).andExpect(status().isOk());
        String heroeB = heroeDe(matchId, equipoB);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/partidas/{m}/reto/clicks", matchId).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"" + heroeB + "\"}"))
                    .andExpect(status().isNoContent());
        }
        challenges.finish(matchId);

        // B resuelve 1 acertijo y A los resuelve todos (el acertijo-1 llega repetido: no cuenta doble)
        events.publishEvent(new AcertijoResueltoEvent(matchId, equipoB, "acertijo-1", "caro"));
        for (String acertijo : new String[]{"acertijo-1", "acertijo-1", "acertijo-2", "acertijo-3"}) {
            events.publishEvent(new AcertijoResueltoEvent(matchId, equipoA, acertijo, "ana"));
        }

        mvc.perform(get("/api/partidas/{m}/resultado", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo").value("ACERTIJOS_COMPLETOS"))
                .andExpect(jsonPath("$.equipoGanadorId").value(equipoA))
                .andExpect(jsonPath("$.clasificacion[0].equipoId").value(equipoA))
                .andExpect(jsonPath("$.clasificacion[0].acertijosResueltos").value(3))
                .andExpect(jsonPath("$.clasificacion[0].puntaje").value(3))
                .andExpect(jsonPath("$.clasificacion[1].retosGanados").value(1))
                .andExpect(jsonPath("$.clasificacion[1].puntaje").value(2));

        mvc.perform(get("/api/partidas/{m}/cronometro", matchId)).andExpect(status().isNotFound());
        mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", matchId, "ana")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"x\":1,\"y\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("termino")));

        // Salas (borra la sala) y social (ranking) reciben el fin con el ganador y los miembros de cada equipo
        await().atMost(Duration.ofSeconds(10)).until(entreServicios::entregados);
        MatchFinishedEvent fin = fines.de(matchId).orElseThrow();
        assertEquals(teamOf.get("ana"), fin.winnerTeamId());
        assertEquals(java.util.List.of("ana", "beto"), fin.equipos().get(teamOf.get("ana")));
        mvc.perform(get("/api/partidas/{m}/resultado", matchId)).andExpect(status().isOk());
    }

    @Test
    void siSeAcabaElTiempoIgualadosEsEmpate() throws Exception {
        Map<String, String> teamOf = new LinkedHashMap<>();
        String matchId = partidaIniciada("Sala empate", teamOf);

        events.publishEvent(new AcertijoResueltoEvent(matchId, teamOf.get("ana"), "acertijo-1", "ana"));
        events.publishEvent(new AcertijoResueltoEvent(matchId, teamOf.get("caro"), "acertijo-2", "caro"));
        events.publishEvent(new TimeUpEvent(matchId, Instant.now()));

        mvc.perform(get("/api/partidas/{m}/resultado", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo").value("TIEMPO_AGOTADO"))
                .andExpect(jsonPath("$.empate").value(true))
                .andExpect(jsonPath("$.equipoGanadorId").doesNotExist())
                .andExpect(jsonPath("$.equiposEmpatados", hasSize(2)));
    }

    private String heroeDe(String matchId, String equipoId) throws Exception {
        JsonNode roles = json.readTree(mvc.perform(get("/api/partidas/{m}/equipos/{e}/roles", matchId, equipoId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("roles");
        var it = roles.fields();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getValue().asText().equals("HEROE")) return e.getKey();
        }
        throw new AssertionError("El equipo " + equipoId + " no tiene heroe");
    }

    /** Lo que publica salas al arrancar: dos equipos (ana y beto, caro y dani) y 10 minutos de partida. */
    private String partidaIniciada(String nombre, Map<String, String> teamOf) {
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
}
