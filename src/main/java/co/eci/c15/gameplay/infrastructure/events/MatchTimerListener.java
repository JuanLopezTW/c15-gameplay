package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.MatchTimerService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


/** Starts the match timer when the match begins (HU-34.5 + HU-64.4). */
@Component
public class MatchTimerListener {

    private final MatchTimerService timers;

    public MatchTimerListener(MatchTimerService timers) {
        this.timers = timers;
    }

    /** La partida dura lo que eligio el anfitrion (viene en el evento). */
    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        timers.start(event.matchId(), event.duracion());
    }
}
