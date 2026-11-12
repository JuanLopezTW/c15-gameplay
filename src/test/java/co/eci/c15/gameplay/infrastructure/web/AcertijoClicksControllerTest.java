package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ClickAcertijoUseCase;
import co.eci.c15.gameplay.application.EntradaAcertijoDto;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoNoAbiertoException;
import co.eci.c15.gameplay.domain.AcertijoYaResueltoException;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.ElementoNoValidoException;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AcertijoClicksControllerTest {

    private static final String URL = "/api/partidas/m1/acertijos/acertijo-1/clicks";

    private ClickAcertijoUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(ClickAcertijoUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new AcertijoClicksController(useCase)).build();
    }

    private static String cuerpo(String json) { return json; }

    @Test
    void clickValidoDevuelveLaEntradaAcumulada() throws Exception {
        when(useCase.click("m1", "acertijo-1", "ana", "lista", "rojo"))
                .thenReturn(new EntradaAcertijoDto("acertijo-1", List.of("rojo"), false));

        mvc.perform(post(URL).principal(() -> "ana").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("{\"userId\":\"ana\",\"elementoId\":\"lista\",\"valor\":\"rojo\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrada[0]").value("rojo"))
                .andExpect(jsonPath("$.resuelto").value(false));
    }

    @Test
    void traduceLasExcepcionesDelDominioAEstadosHttp() throws Exception {
        String ok = "{\"userId\":\"ana\",\"elementoId\":\"lista\"}";
        when(useCase.click(eq("m1"), eq("acertijo-1"), eq("ana"), any(), any()))
                .thenThrow(new JugadorNoRegistradoException("m1", "ana"))
                .thenThrow(new ComponenteSinAcertijoException("acertijo-1"))
                .thenThrow(new AcertijoNoAbiertoException("acertijo-1", "ana"))
                .thenThrow(new AcertijoBloqueadoException("acertijo-1"))
                .thenThrow(new AcertijoYaResueltoException("acertijo-1"))
                .thenThrow(new ElementoNoValidoException("acertijo-1", "x"))
                .thenThrow(new IllegalArgumentException("valor largo"));

        int[] esperados = {404, 404, 409, 409, 409, 400, 400};
        for (int esperado : esperados) {
            mvc.perform(post(URL).principal(() -> "ana").contentType(MediaType.APPLICATION_JSON).content(ok))
                    .andExpect(status().is(esperado))
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Test
    void consultarYReiniciarDelegan() throws Exception {
        when(useCase.consultar("m1", "acertijo-1", "ana"))
                .thenReturn(new EntradaAcertijoDto("acertijo-1", List.of("rojo", "azul"), false));
        when(useCase.reiniciar("m1", "acertijo-1", "ana"))
                .thenReturn(new EntradaAcertijoDto("acertijo-1", List.of(), false));

        mvc.perform(get(URL).principal(() -> "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrada[1]").value("azul"));
        mvc.perform(delete(URL).principal(() -> "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrada").isEmpty());
        verify(useCase).reiniciar("m1", "acertijo-1", "ana");
    }
}
