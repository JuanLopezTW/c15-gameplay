package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ResultadoSolucionDto;
import co.eci.c15.gameplay.application.ValidarSolucionUseCase;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoNoAbiertoException;
import co.eci.c15.gameplay.domain.AcertijoYaResueltoException;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.SolucionNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AcertijoSolucionControllerTest {

    private static final String URL = "/api/partidas/m1/acertijos/acertijo-1/solucion";

    private ValidarSolucionUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(ValidarSolucionUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new AcertijoSolucionController(useCase)).build();
    }

    @Test
    void solucionCorrectaDevuelve200ConElResultado() throws Exception {
        when(useCase.ejecutar("m1", "acertijo-1", "ana", List.of("16")))
                .thenReturn(new ResultadoSolucionDto(true, true, "ok"));

        mvc.perform(post(URL).principal(() -> "ana").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ana\",\"respuesta\":[\"16\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correcto").value(true))
                .andExpect(jsonPath("$.resuelto").value(true));
    }

    @Test
    void solucionIncorrectaTambienDevuelve200ConCorrectoFalso() throws Exception {
        when(useCase.ejecutar("m1", "acertijo-1", "ana", null))
                .thenReturn(new ResultadoSolucionDto(false, false, "Incorrecto"));

        mvc.perform(post(URL).principal(() -> "ana").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correcto").value(false))
                .andExpect(jsonPath("$.resuelto").value(false));
    }

    @Test
    void traduceLasExcepcionesDelDominioAEstadosHttp() throws Exception {
        when(useCase.ejecutar(eq("m1"), eq("acertijo-1"), eq("ana"), any()))
                .thenThrow(new JugadorNoRegistradoException("m1", "ana"))
                .thenThrow(new ComponenteSinAcertijoException("acertijo-1"))
                .thenThrow(new SolucionNoDisponibleException("acertijo-1"))
                .thenThrow(new AcertijoNoAbiertoException("acertijo-1", "ana"))
                .thenThrow(new AcertijoBloqueadoException("acertijo-1"))
                .thenThrow(new AcertijoYaResueltoException("acertijo-1"))
                .thenThrow(new IllegalArgumentException("respuesta muy larga"));

        int[] esperados = {404, 404, 404, 409, 409, 409, 400};
        for (int esperado : esperados) {
            mvc.perform(post(URL).principal(() -> "ana").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"ana\"}"))
                    .andExpect(status().is(esperado))
                    .andExpect(jsonPath("$.error").exists());
        }
    }
}
