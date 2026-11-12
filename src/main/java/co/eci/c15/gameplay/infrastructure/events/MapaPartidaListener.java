package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.ObtenerMapaUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MapaPartidaListener {

    private final ObtenerMapaUseCase obtenerMapa;

    public MapaPartidaListener(ObtenerMapaUseCase obtenerMapa) {
        this.obtenerMapa = obtenerMapa;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        obtenerMapa.ejecutar(event.matchId());
    }
}
