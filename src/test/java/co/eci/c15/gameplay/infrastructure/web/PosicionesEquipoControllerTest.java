package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ConsultarPosicionesEquipoUseCase;
import co.eci.c15.gameplay.application.DesconectarJugadorUseCase;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PosicionesEquipoControllerTest {

    private JugadorEnMapaRepositoryEnMemoria jugadores;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jugadores = new JugadorEnMapaRepositoryEnMemoria();
        jugadores.saveIfAbsent(new JugadorEnMapa("p1", "e1", "a1", new Posicion(1, 1)));
        jugadores.saveIfAbsent(new JugadorEnMapa("p1", "e1", "a2", new Posicion(2, 2)));
        jugadores.saveIfAbsent(new JugadorEnMapa("p1", "e2", "b1", new Posicion(5, 5)));
        mvc = MockMvcBuilders.standaloneSetup(new PosicionesEquipoController(
                new ConsultarPosicionesEquipoUseCase(jugadores),
                new DesconectarJugadorUseCase(jugadores, (m, e, dto) -> { }))).build();
    }

    @Test
    void miembroDelEquipoVeLasPosicionesDeSusCompaneros() throws Exception {
        mvc.perform(get("/api/partidas/p1/equipos/e1/posiciones").principal(() -> "a1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].userId").value("a2"))
                .andExpect(jsonPath("$[1].x").value(2))
                .andExpect(jsonPath("$[1].conectado").value(true));
    }

    @Test
    void jugadorDeOtroEquipoRecibe403() throws Exception {
        mvc.perform(get("/api/partidas/p1/equipos/e1/posiciones").principal(() -> "b1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void jugadorDesconocidoRecibe404() throws Exception {
        mvc.perform(get("/api/partidas/p1/equipos/e1/posiciones").principal(() -> "nadie"))
                .andExpect(status().isNotFound());
    }

    @Test
    void desconexionExplicitaSacaAlJugador() throws Exception {
        mvc.perform(delete("/api/partidas/p1/jugadores/a1").principal(() -> "a1")).andExpect(status().isNoContent());
        assertTrue(jugadores.find("p1", "a1").isEmpty());
        mvc.perform(delete("/api/partidas/p1/jugadores/a1").principal(() -> "a1")).andExpect(status().isNoContent());
    }
}
