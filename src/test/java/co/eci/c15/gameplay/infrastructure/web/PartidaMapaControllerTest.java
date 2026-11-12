package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ObtenerMapaUseCase;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.MapaNoGeneradoException;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PartidaMapaControllerTest {

    private MockMvc mvc(GeneradorMapa generador) {
        ObtenerMapaUseCase useCase = new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), generador);
        return MockMvcBuilders.standaloneSetup(new PartidaMapaController(useCase)).build();
    }

    @Test
    void devuelveElMapaDeLaPartida() throws Exception {
        mvc(new GeneradorMapa()).perform(get("/api/partidas/partida-1/mapa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value("partida-1"))
                .andExpect(jsonPath("$.ancho").value(12))
                .andExpect(jsonPath("$.spawn.x").value(0))
                .andExpect(jsonPath("$.componentes", hasSize(12)));
    }

    @Test
    void dosPeticionesDeLaMismaPartidaVenElMismoMapa() throws Exception {
        MockMvc mvc = mvc(new GeneradorMapa());
        String primero = mvc.perform(get("/api/partidas/partida-1/mapa")).andReturn().getResponse().getContentAsString();
        String segundo = mvc.perform(get("/api/partidas/partida-1/mapa")).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(primero, segundo);
    }

    @Test
    void peticionInvalidaDevuelve400ConMensaje() throws Exception {
        GeneradorMapa rechaza = new GeneradorMapa() {
            @Override
            public MapaIsometrico generar(String matchId) {
                throw new IllegalArgumentException("partida invalida");
            }
        };
        mvc(rechaza).perform(get("/api/partidas/partida-1/mapa"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("partida invalida"));
    }

    @Test
    void errorDeCargaDelMapaDevuelve500ConMensaje() throws Exception {
        GeneradorMapa fallido = new GeneradorMapa() {
            @Override
            public MapaIsometrico generar(String matchId) {
                throw new MapaNoGeneradoException("No se pudo cargar el mapa");
            }
        };
        mvc(fallido).perform(get("/api/partidas/partida-1/mapa"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("No se pudo cargar el mapa"));
    }
}
