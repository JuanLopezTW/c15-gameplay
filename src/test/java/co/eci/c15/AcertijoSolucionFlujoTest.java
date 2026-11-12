package co.eci.c15;

import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
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
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Resolver acertijos con la app ensamblada: catalogo, soluciones y mapa reales. */
@SpringBootTest
@AutoConfigureMockMvc
class AcertijoSolucionFlujoTest {

    private static final String MATCH = "match-solucion-flujo";

    @TestConfiguration
    static class Eventos {
        @Bean
        Recolector recolector() { return new Recolector(); }
    }

    static class Recolector {
        final List<AcertijoResueltoEvent> resueltos = new CopyOnWriteArrayList<>();

        @EventListener
        void on(AcertijoResueltoEvent e) { resueltos.add(e); }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private ObtenerMapaUseCase obtenerMapa;

    @Autowired
    private Recolector recolector;

    @Test
    void resolverUnAcertijoPorClicksDeLetras() throws Exception {
        events.publishEvent(new PartidaIniciadaEvent(MATCH, Map.of(
                "equipo-1", List.of("ana", "beto"), "equipo-2", List.of("carla", "dani"))));
        MapaIsometrico mapa = obtenerMapa.obtener(MATCH);
        ComponenteMapa acertijo = mapa.getComponentes().stream()
                .filter(c -> c.id().equals("acertijo-3")).findFirst().orElseThrow();
        Posicion junto = celdaVecinaAlcanzable(mapa, acertijo.posicion());
        for (String jugador : List.of("ana", "beto", "carla")) mover(jugador, junto).andExpect(status().isOk());

        // sin abrirlo no se puede resolver
        solucion("ana", null).andExpect(status().isConflict());

        abrir("ana").andExpect(status().isOk());
        for (String letra : List.of("X", "S", "C", "A", "P", "E")) click("ana", letra).andExpect(status().isOk());
        solucion("ana", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.correcto").value(false))
                .andExpect(jsonPath("$.resuelto").value(false));

        // el companero sigue bloqueado; el otro equipo tiene su propio acertijo
        solucion("beto", "[\"escape\"]").andExpect(status().isConflict());
        abrir("carla").andExpect(status().isOk());

        // reintento correcto
        for (String letra : List.of("E", "S", "C", "A", "P", "E")) click("ana", letra).andExpect(status().isOk());
        solucion("ana", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.correcto").value(true))
                .andExpect(jsonPath("$.resuelto").value(true));

        assertEquals(List.of(new AcertijoResueltoEvent(MATCH, "equipo-1", "acertijo-3", "ana")),
                recolector.resueltos.stream().filter(e -> e.matchId().equals(MATCH)).toList());

        // ya resuelto: no se reenvia, el companero lo ve resuelto y el otro equipo no
        solucion("ana", "[\"escape\"]").andExpect(status().isConflict());
        abrir("beto").andExpect(status().isOk()).andExpect(jsonPath("$.resuelto").value(true));
        abrir("carla").andExpect(status().isOk()).andExpect(jsonPath("$.resuelto").value(false));
        solucion("carla", "[\"Escape\"]").andExpect(status().isOk()).andExpect(jsonPath("$.correcto").value(true));

        // quien resolvio puede volver a moverse
        mover("ana", mapa.getSpawn()).andExpect(status().isOk());
    }

    @Test
    void respuestaEnviadaDirectamenteYErrores() throws Exception {
        String match = MATCH + "-directa";
        events.publishEvent(new PartidaIniciadaEvent(match, Map.of(
                "equipo-1", List.of("eva", "fede"), "equipo-2", List.of("gabi", "hugo"))));
        MapaIsometrico mapa = obtenerMapa.obtener(match);
        ComponenteMapa acertijo = mapa.getComponentes().stream()
                .filter(c -> c.id().equals("acertijo-1")).findFirst().orElseThrow();
        mvc.perform(post("/api/partidas/{m}/jugadores/eva/movimiento", match).contentType(MediaType.APPLICATION_JSON)
                .content(coords(celdaVecinaAlcanzable(mapa, acertijo.posicion())))).andExpect(status().isOk());
        mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-1/bloqueo", match)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"eva\"}")).andExpect(status().isOk());

        mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-1/solucion", match).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"eva\",\"respuesta\":[\"16\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correcto").value(true));

        mvc.perform(post("/api/partidas/{m}/acertijos/llave-1/solucion", match).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"eva\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-1/solucion", match).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"intruso\"}")).andExpect(status().isNotFound());
    }

    private ResultActions solucion(String userId, String respuestaJson) throws Exception {
        String cuerpo = respuestaJson == null
                ? "{\"userId\":\"" + userId + "\"}"
                : "{\"userId\":\"" + userId + "\",\"respuesta\":" + respuestaJson + "}";
        return mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-3/solucion", MATCH)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions click(String userId, String valor) throws Exception {
        return mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-3/clicks", MATCH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + userId + "\",\"elementoId\":\"placeholder-acertijo-3-elemento-1\",\"valor\":\"" + valor + "\"}"));
    }

    private ResultActions abrir(String userId) throws Exception {
        return mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-3/bloqueo", MATCH)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + userId + "\"}"));
    }

    private ResultActions mover(String userId, Posicion destino) throws Exception {
        return mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", MATCH, userId)
                .contentType(MediaType.APPLICATION_JSON).content(coords(destino)));
    }

    private static String coords(Posicion p) {
        return "{\"x\":" + p.x() + ",\"y\":" + p.y() + "}";
    }

    private static Posicion celdaVecinaAlcanzable(MapaIsometrico mapa, Posicion p) {
        return Stream.of(new Posicion(p.x() + 1, p.y()), new Posicion(p.x() - 1, p.y()),
                        new Posicion(p.x(), p.y() + 1), new Posicion(p.x(), p.y() - 1))
                .filter(v -> mapa.rutaEntre(mapa.getSpawn(), v).isPresent())
                .findFirst().orElseThrow();
    }
}
