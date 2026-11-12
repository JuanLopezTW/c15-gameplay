package co.eci.c15;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.ObtenerMapaUseCase;
import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Flujo de acertijos contra la app ensamblada: partida real, mapa real y catálogo placeholder. */
@SpringBootTest
@AutoConfigureMockMvc
class AcertijosFlujoTest {

    private static final String MATCH = "match-acertijos-flujo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private ObtenerMapaUseCase obtenerMapa;

    @Test
    void abrirBloquearYCerrarPorEquipo() throws Exception {
        events.publishEvent(new PartidaIniciadaEvent(MATCH, Map.of(
                "equipo-1", List.of("ana", "beto"),
                "equipo-2", List.of("carla", "dani"))));
        MapaIsometrico mapa = obtenerMapa.obtener(MATCH);
        ComponenteMapa acertijo = mapa.getComponentes().stream()
                .filter(c -> c.id().equals("acertijo-1")).findFirst().orElseThrow();
        Posicion junto = celdaVecinaAlcanzable(mapa, acertijo.posicion());

        // Desde el spawn está lejos
        abrir("ana").andExpect(status().isBadRequest());

        for (String jugador : List.of("ana", "beto", "carla")) mover(jugador, junto).andExpect(status().isOk());

        abrir("ana").andExpect(status().isOk())
                .andExpect(jsonPath("$.componenteMapaId").value("acertijo-1"))
                .andExpect(jsonPath("$.enunciado").isNotEmpty())
                .andExpect(jsonPath("$.elementos").isNotEmpty());

        abrir("beto").andExpect(status().isConflict());
        abrir("carla").andExpect(status().isOk());
        mover("ana", mapa.getSpawn()).andExpect(status().isBadRequest());

        mvc.perform(delete("/api/partidas/{m}/acertijos/acertijo-1/bloqueo", MATCH).param("userId", "ana"))
                .andExpect(status().isNoContent());

        abrir("beto").andExpect(status().isOk());
        mover("ana", mapa.getSpawn()).andExpect(status().isOk());
    }

    @Test
    void componenteSinAcertijoYJugadorAjenoDan404() throws Exception {
        events.publishEvent(new PartidaIniciadaEvent(MATCH + "-404", Map.of(
                "equipo-1", List.of("eva", "fede"), "equipo-2", List.of("gabi", "hugo"))));

        mvc.perform(post("/api/partidas/{m}/acertijos/{c}/bloqueo", MATCH + "-404", "llave-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"eva\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/partidas/{m}/acertijos/{c}/bloqueo", MATCH + "-404", "acertijo-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"intruso\"}"))
                .andExpect(status().isNotFound());
    }

    private ResultActions abrir(String userId) throws Exception {
        return mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-1/bloqueo", MATCH)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + userId + "\"}"));
    }

    private ResultActions mover(String userId, Posicion destino) throws Exception {
        return mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", MATCH, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"x\":" + destino.x() + ",\"y\":" + destino.y() + "}"));
    }

    private static Posicion celdaVecinaAlcanzable(MapaIsometrico mapa, Posicion p) {
        return Stream.of(new Posicion(p.x() + 1, p.y()), new Posicion(p.x() - 1, p.y()),
                        new Posicion(p.x(), p.y() + 1), new Posicion(p.x(), p.y() - 1))
                .filter(v -> mapa.rutaEntre(mapa.getSpawn(), v).isPresent())
                .findFirst().orElseThrow();
    }
}
