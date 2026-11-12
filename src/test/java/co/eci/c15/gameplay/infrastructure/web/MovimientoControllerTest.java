package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.CandadoEquipo;
import co.eci.c15.gameplay.application.MoverPersonajeUseCase;
import co.eci.c15.gameplay.application.ObtenerMapaUseCase;
import co.eci.c15.gameplay.application.RegistrarJugadoresUseCase;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovimientoControllerTest {

    private MockMvc mvc;
    private MapaIsometrico mapa;
    private Posicion libre;

    @BeforeEach
    void setUp() {
        JugadorEnMapaRepositoryEnMemoria jugadores = new JugadorEnMapaRepositoryEnMemoria();
        ObtenerMapaUseCase obtenerMapa = new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), new GeneradorMapa());
        new RegistrarJugadoresUseCase(jugadores, obtenerMapa).ejecutar("p1", Map.of("e1", List.of("u1")));
        mvc = MockMvcBuilders.standaloneSetup(
                new MovimientoController(new MoverPersonajeUseCase(jugadores, obtenerMapa, e -> { }, CandadoEquipo.ninguno()))).build();
        mapa = obtenerMapa.obtener("p1");
        libre = new Posicion(mapa.getSpawn().x() + 1, mapa.getSpawn().y());
        if (!mapa.esTransitable(libre)) libre = new Posicion(mapa.getSpawn().x(), mapa.getSpawn().y() + 1);
    }

    private static String cuerpo(int x, int y) {
        return "{\"x\":" + x + ",\"y\":" + y + "}";
    }

    @Test
    void movimientoValidoDevuelveRuta() throws Exception {
        mvc.perform(post("/api/partidas/p1/jugadores/u1/movimiento").principal(() -> "u1")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(libre.x(), libre.y())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.hasta.x").value(libre.x()))
                .andExpect(jsonPath("$.ruta[0].x").value(libre.x()));
    }

    @Test
    void clickFueraDelMapaDevuelve400() throws Exception {
        mvc.perform(post("/api/partidas/p1/jugadores/u1/movimiento").principal(() -> "u1")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(50, 50)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void clickSobreComponenteDevuelve400() throws Exception {
        Posicion ocupada = mapa.getComponentes().get(0).posicion();
        mvc.perform(post("/api/partidas/p1/jugadores/u1/movimiento").principal(() -> "u1")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(ocupada.x(), ocupada.y())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jugadorDesconocidoDevuelve404() throws Exception {
        mvc.perform(post("/api/partidas/p1/jugadores/nadie/movimiento").principal(() -> "nadie")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(libre.x(), libre.y())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
