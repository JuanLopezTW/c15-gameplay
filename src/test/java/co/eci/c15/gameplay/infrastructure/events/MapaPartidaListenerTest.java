package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.ObtenerMapaUseCase;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MapaPartidaListenerTest {

    @Test
    void alIniciarLaPartidaQuedaGeneradoElMapa() {
        MapaRepositoryEnMemoria repo = new MapaRepositoryEnMemoria();
        MapaPartidaListener listener = new MapaPartidaListener(new ObtenerMapaUseCase(repo, new GeneradorMapa()));

        listener.onPartidaIniciada(new PartidaIniciadaEvent("partida-1", Map.of("e1", List.of("u1", "u2"))));

        assertTrue(repo.findByMatchId("partida-1").isPresent());
    }
}
