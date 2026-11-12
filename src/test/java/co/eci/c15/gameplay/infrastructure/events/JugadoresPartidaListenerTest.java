package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.ObtenerMapaUseCase;
import co.eci.c15.gameplay.application.RegistrarJugadoresUseCase;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JugadoresPartidaListenerTest {

    @Test
    void alIniciarLaPartidaTodosLosJugadoresQuedanEnElMapa() {
        JugadorEnMapaRepositoryEnMemoria jugadores = new JugadorEnMapaRepositoryEnMemoria();
        ObtenerMapaUseCase obtenerMapa = new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), new GeneradorMapa());
        JugadoresPartidaListener listener = new JugadoresPartidaListener(new RegistrarJugadoresUseCase(jugadores, obtenerMapa));

        listener.onPartidaIniciada(new PartidaIniciadaEvent("p1",
                Map.of("e1", List.of("u1", "u2"), "e2", List.of("u3"))));

        assertEquals(3, jugadores.findByMatchId("p1").size());
        assertEquals("e2", jugadores.find("p1", "u3").orElseThrow().getEquipoId());
        assertTrue(jugadores.find("otra", "u1").isEmpty());
    }
}
