package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.ProgresoPartidaUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProgresoPartidaListener {

    private final ProgresoPartidaUseCase progreso;

    public ProgresoPartidaListener(ProgresoPartidaUseCase progreso) {
        this.progreso = progreso;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        progreso.iniciar(event.matchId(), event.equipos().keySet());
    }

    @EventListener
    public void onAcertijoResuelto(AcertijoResueltoEvent event) {
        progreso.acertijoResuelto(event);
    }
}
