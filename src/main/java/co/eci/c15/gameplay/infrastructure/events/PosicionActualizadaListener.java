package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.gameplay.application.PosicionActualizadaEvent;
import co.eci.c15.gameplay.application.PosicionJugadorDto;
import co.eci.c15.gameplay.application.PosicionNotifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PosicionActualizadaListener {

    private final PosicionNotifier notifier;

    public PosicionActualizadaListener(PosicionNotifier notifier) {
        this.notifier = notifier;
    }

    @EventListener
    public void onPosicionActualizada(PosicionActualizadaEvent event) {
        notifier.notificar(event.matchId(), event.equipoId(),
                PosicionJugadorDto.movimiento(event.userId(), event.posicion(), event.ruta()));
    }
}
