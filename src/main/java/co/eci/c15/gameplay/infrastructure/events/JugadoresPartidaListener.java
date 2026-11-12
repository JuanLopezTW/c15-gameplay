package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.RegistrarJugadoresUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class JugadoresPartidaListener {

    private final RegistrarJugadoresUseCase registrar;

    public JugadoresPartidaListener(RegistrarJugadoresUseCase registrar) {
        this.registrar = registrar;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        registrar.ejecutar(event.matchId(), event.equipos());
    }
}
