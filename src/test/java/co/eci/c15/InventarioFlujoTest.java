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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Inventario compartido con la app ensamblada: partida, mapa y movimiento reales. */
@SpringBootTest
@AutoConfigureMockMvc
class InventarioFlujoTest {

    private static final String MATCH = "match-inventario-flujo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private ObtenerMapaUseCase obtenerMapa;

    @Test
    void lasLlavesSeComparteSoloDentroDelEquipo() throws Exception {
        events.publishEvent(new PartidaIniciadaEvent(MATCH, Map.of(
                "equipo-1", List.of("ana", "beto"), "equipo-2", List.of("carla", "dani"))));
        MapaIsometrico mapa = obtenerMapa.obtener(MATCH);
        Posicion junto = junto(mapa, "llave-1");

        // lejos del objeto no se puede recolectar
        recolectar("ana", "llave-1").andExpect(status().isBadRequest());

        for (String jugador : List.of("ana", "beto", "carla")) mover(jugador, junto).andExpect(status().isOk());

        recolectar("ana", "llave-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.objetos[0].id").value("llave-1"))
                .andExpect(jsonPath("$.objetos[0].recolectadoPor").value("ana"));

        // el companero ve el mismo inventario; no puede volver a recolectarla
        inventario("equipo-1", "beto").andExpect(status().isOk())
                .andExpect(jsonPath("$.objetos.length()").value(1))
                .andExpect(jsonPath("$.objetos[0].nombre").value("Llave 1"));
        recolectar("beto", "llave-1").andExpect(status().isConflict());

        // el otro equipo tiene su propio inventario y su propia llave
        inventario("equipo-2", "carla").andExpect(status().isOk()).andExpect(jsonPath("$.objetos").isEmpty());
        recolectar("carla", "llave-1").andExpect(status().isOk());
        inventario("equipo-1", "ana").andExpect(jsonPath("$.objetos.length()").value(1));

        // privacidad entre equipos
        inventario("equipo-1", "carla").andExpect(status().isForbidden());
    }

    @Test
    void objetosQueNoSonRecolectablesYJugadoresAjenos() throws Exception {
        String match = MATCH + "-errores";
        events.publishEvent(new PartidaIniciadaEvent(match, Map.of(
                "equipo-1", List.of("eva", "fede"), "equipo-2", List.of("gabi", "hugo"))));

        mvc.perform(post("/api/partidas/{m}/objetos/acertijo-1/recoleccion", match)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"eva\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/partidas/{m}/objetos/llave-1/recoleccion", match)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"intruso\"}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/partidas/{m}/equipos/equipo-1/inventario", match).param("userId", "intruso"))
                .andExpect(status().isNotFound());
    }

    private ResultActions recolectar(String userId, String objeto) throws Exception {
        return mvc.perform(post("/api/partidas/{m}/objetos/{o}/recoleccion", MATCH, objeto)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + userId + "\"}"));
    }

    private ResultActions inventario(String equipo, String userId) throws Exception {
        return mvc.perform(get("/api/partidas/{m}/equipos/{e}/inventario", MATCH, equipo).param("userId", userId));
    }

    private ResultActions mover(String userId, Posicion destino) throws Exception {
        return mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", MATCH, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"x\":" + destino.x() + ",\"y\":" + destino.y() + "}"));
    }

    private static Posicion junto(MapaIsometrico mapa, String componenteId) {
        ComponenteMapa c = mapa.getComponentes().stream().filter(x -> x.id().equals(componenteId)).findFirst().orElseThrow();
        Posicion p = c.posicion();
        return Stream.of(new Posicion(p.x() + 1, p.y()), new Posicion(p.x() - 1, p.y()),
                        new Posicion(p.x(), p.y() + 1), new Posicion(p.x(), p.y() - 1))
                .filter(v -> mapa.rutaEntre(mapa.getSpawn(), v).isPresent())
                .findFirst().orElseThrow();
    }
}
