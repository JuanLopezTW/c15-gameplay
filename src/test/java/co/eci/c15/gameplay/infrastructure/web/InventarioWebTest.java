package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ConsultarInventarioUseCase;
import co.eci.c15.gameplay.application.InventarioDto;
import co.eci.c15.gameplay.application.RecolectarObjetoUseCase;
import co.eci.c15.gameplay.application.AccesoEquipoDenegadoException;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.ObjetoLejosException;
import co.eci.c15.gameplay.domain.ObjetoNoRecolectableException;
import co.eci.c15.gameplay.domain.ObjetoYaRecolectadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventarioWebTest {

    private static final String RECOLECTAR = "/api/partidas/m1/objetos/llave-1/recoleccion";
    private static final String INVENTARIO = "/api/partidas/m1/equipos/e1/inventario";

    private RecolectarObjetoUseCase recolectar;
    private ConsultarInventarioUseCase consultar;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        recolectar = mock(RecolectarObjetoUseCase.class);
        consultar = mock(ConsultarInventarioUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new InventarioController(recolectar, consultar)).build();
    }

    private static final InventarioDto INV = new InventarioDto("m1", "e1",
            List.of(new InventarioDto.ObjetoDto("llave-1", "Llave 1", "ana")));

    @Test
    void recolectarDevuelveElInventarioActualizado() throws Exception {
        when(recolectar.ejecutar("m1", "ana", "llave-1")).thenReturn(INV);

        mvc.perform(post(RECOLECTAR).principal(() -> "ana").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipoId").value("e1"))
                .andExpect(jsonPath("$.objetos[0].id").value("llave-1"))
                .andExpect(jsonPath("$.objetos[0].nombre").value("Llave 1"));
    }

    @Test
    void traduceLasExcepcionesDeRecoleccionAEstadosHttp() throws Exception {
        when(recolectar.ejecutar("m1", "ana", "llave-1"))
                .thenThrow(new JugadorNoRegistradoException("m1", "ana"))
                .thenThrow(new ObjetoNoRecolectableException("llave-1"))
                .thenThrow(new ObjetoLejosException("llave-1"))
                .thenThrow(new ObjetoYaRecolectadoException("llave-1"));

        int[] esperados = {404, 404, 400, 409};
        for (int esperado : esperados) {
            mvc.perform(post(RECOLECTAR).principal(() -> "ana").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"ana\"}"))
                    .andExpect(status().is(esperado))
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Test
    void consultarDevuelveElInventarioYTraduceErrores() throws Exception {
        when(consultar.ejecutar("m1", "e1", "ana")).thenReturn(INV);
        when(consultar.ejecutar("m1", "e1", "carla")).thenThrow(new AccesoEquipoDenegadoException("carla", "e1"));
        when(consultar.ejecutar("m1", "e1", "intruso")).thenThrow(new JugadorNoRegistradoException("m1", "intruso"));

        mvc.perform(get(INVENTARIO).principal(() -> "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objetos.length()").value(1));
        mvc.perform(get(INVENTARIO).principal(() -> "carla")).andExpect(status().isForbidden());
        mvc.perform(get(INVENTARIO).principal(() -> "intruso")).andExpect(status().isNotFound());
    }

    @Test
    void stompPublicaEnElTopicDeInventarioDelEquipo() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);

        new StompInventarioNotifier(messaging).notificar(INV);

        verify(messaging).convertAndSend("/topic/partidas/m1/equipos/e1/inventario", INV);
        verifyNoMoreInteractions(messaging);
    }
}
