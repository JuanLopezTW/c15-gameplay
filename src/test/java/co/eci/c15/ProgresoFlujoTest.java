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

/** El progreso se actualiza solo cuando un equipo resuelve un acertijo de verdad, con la app ensamblada. */
@SpringBootTest
@AutoConfigureMockMvc
class ProgresoFlujoTest {

    private static final String MATCH = "match-progreso-flujo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private ObtenerMapaUseCase obtenerMapa;

    @Test
    void resolverUnAcertijoActualizaElProgresoDeSuEquipo() throws Exception {
        events.publishEvent(new PartidaIniciadaEvent(MATCH, Map.of(
                "equipo-1", List.of("ana", "beto"), "equipo-2", List.of("carla", "dani"))));

        mvc.perform(get("/api/partidas/{m}/progreso", MATCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipos.length()").value(2))
                .andExpect(jsonPath("$.equipos[0].resueltos").value(0))
                .andExpect(jsonPath("$.equipos[0].total").value(3));

        resolver("ana", "acertijo-1", "[\"16\"]");

        mvc.perform(get("/api/partidas/{m}/progreso", MATCH))
                .andExpect(jsonPath("$.equipos[0].equipoId").value("equipo-1"))
                .andExpect(jsonPath("$.equipos[0].resueltos").value(1))
                .andExpect(jsonPath("$.equipos[0].acertijosResueltos[0]").value("acertijo-1"))
                .andExpect(jsonPath("$.equipos[1].resueltos").value(0));

        // un intento incorrecto no suma
        abrirYAcercar("ana", "acertijo-2");
        mvc.perform(post("/api/partidas/{m}/acertijos/acertijo-2/solucion", MATCH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"ana\",\"respuesta\":[\"azul\"]}"))
                .andExpect(jsonPath("$.correcto").value(false));
        mvc.perform(get("/api/partidas/{m}/progreso", MATCH)).andExpect(jsonPath("$.equipos[0].resueltos").value(1));

        // el otro equipo avanza por su cuenta
        resolver("carla", "acertijo-3", "[\"escape\"]");
        mvc.perform(get("/api/partidas/{m}/progreso", MATCH))
                .andExpect(jsonPath("$.equipos[0].resueltos").value(1))
                .andExpect(jsonPath("$.equipos[1].resueltos").value(1))
                .andExpect(jsonPath("$.equipos[1].acertijosResueltos[0]").value("acertijo-3"));
    }

    @Test
    void partidaSinProgresoDa404() throws Exception {
        mvc.perform(get("/api/partidas/{m}/progreso", MATCH + "-nada")).andExpect(status().isNotFound());
    }

    private void resolver(String userId, String acertijo, String respuesta) throws Exception {
        abrirYAcercar(userId, acertijo);
        mvc.perform(post("/api/partidas/{m}/acertijos/{a}/solucion", MATCH, acertijo).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"respuesta\":" + respuesta + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correcto").value(true));
    }

    private void abrirYAcercar(String userId, String acertijo) throws Exception {
        MapaIsometrico mapa = obtenerMapa.obtener(MATCH);
        ComponenteMapa componente = mapa.getComponentes().stream()
                .filter(c -> c.id().equals(acertijo)).findFirst().orElseThrow();
        Posicion p = componente.posicion();
        Posicion junto = Stream.of(new Posicion(p.x() + 1, p.y()), new Posicion(p.x() - 1, p.y()),
                        new Posicion(p.x(), p.y() + 1), new Posicion(p.x(), p.y() - 1))
                .filter(v -> mapa.rutaEntre(mapa.getSpawn(), v).isPresent()).findFirst().orElseThrow();
        mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", MATCH, userId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"x\":" + junto.x() + ",\"y\":" + junto.y() + "}")).andExpect(status().isOk());
        ResultActions abrir = mvc.perform(post("/api/partidas/{m}/acertijos/{a}/bloqueo", MATCH, acertijo)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + userId + "\"}"));
        abrir.andExpect(status().isOk());
    }
}
