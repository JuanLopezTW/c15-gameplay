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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Clicks sobre un acertijo con la app ensamblada: partida, mapa, bloqueo y catalogo reales. */
@SpringBootTest
@AutoConfigureMockMvc
class AcertijoClicksFlujoTest {

    private static final String MATCH = "match-clicks-flujo";
    private static final String ELEMENTO = "placeholder-acertijo-2-elemento-1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private ObtenerMapaUseCase obtenerMapa;

    @Test
    void clicksSoloParaQuienTieneElAcertijoAbierto() throws Exception {
        events.publishEvent(new PartidaIniciadaEvent(MATCH, Map.of(
                "equipo-1", List.of("ana", "beto"), "equipo-2", List.of("carla", "dani"))));
        MapaIsometrico mapa = obtenerMapa.obtener(MATCH);
        ComponenteMapa acertijo = mapa.getComponentes().stream()
                .filter(c -> c.id().equals("acertijo-2")).findFirst().orElseThrow();
        Posicion junto = celdaVecinaAlcanzable(mapa, acertijo.posicion());
        for (String jugador : List.of("ana", "beto", "carla")) mover(jugador, junto).andExpect(status().isOk());

        // sin abrir el acertijo no se puede interactuar
        click("ana", "rojo").andExpect(status().isConflict());

        abrir("ana").andExpect(status().isOk());
        click("ana", "rojo").andExpect(status().isOk()).andExpect(jsonPath("$.entrada[0]").value("rojo"));
        click("ana", "naranja").andExpect(status().isOk()).andExpect(jsonPath("$.entrada[1]").value("naranja"));

        // el companero esta bloqueado; el otro equipo tiene su propio acertijo
        click("beto", "azul").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("otro jugador")));
        abrir("carla").andExpect(status().isOk());
        click("carla", "verde").andExpect(status().isOk()).andExpect(jsonPath("$.entrada.length()").value(1));
        mvc.perform(get(clicksUrl()).param("userId", "beto"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entrada.length()").value(2));

        // reiniciar y elemento invalido
        mvc.perform(delete(clicksUrl()).param("userId", "ana"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entrada").isEmpty());
        mvc.perform(post(clicksUrl()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ana\",\"elementoId\":\"no-existe\"}"))
                .andExpect(status().isBadRequest());

        // al cerrar, lo ingresado se descarta y el companero puede abrir
        mvc.perform(delete("/api/partidas/{m}/acertijos/acertijo-2/bloqueo", MATCH).param("userId", "ana"))
                .andExpect(status().isNoContent());
        click("ana", "rojo").andExpect(status().isConflict());
        abrir("beto").andExpect(status().isOk());
        mvc.perform(get(clicksUrl()).param("userId", "beto"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entrada").isEmpty());
    }

    @Test
    void jugadorYAcertijoInexistentesDan404() throws Exception {
        events.publishEvent(new PartidaIniciadaEvent(MATCH + "-404", Map.of(
                "equipo-1", List.of("eva", "fede"), "equipo-2", List.of("gabi", "hugo"))));

        mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-2/clicks", MATCH + "-404")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"intruso\",\"elementoId\":\"" + ELEMENTO + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/partidas/{m}/acertijos/llave-1/clicks", MATCH + "-404")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"eva\",\"elementoId\":\"" + ELEMENTO + "\"}"))
                .andExpect(status().isNotFound());
    }

    private String clicksUrl() {
        return "/api/partidas/" + MATCH + "/acertijos/acertijo-2/clicks";
    }

    private ResultActions click(String userId, String valor) throws Exception {
        return mvc.perform(post(clicksUrl()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + userId + "\",\"elementoId\":\"" + ELEMENTO + "\",\"valor\":\"" + valor + "\"}"));
    }

    private ResultActions abrir(String userId) throws Exception {
        return mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-2/bloqueo", MATCH)
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
