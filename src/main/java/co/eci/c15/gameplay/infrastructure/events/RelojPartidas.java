package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import co.eci.c15.gameplay.application.MatchTimerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Un solo reloj por instancia, en vez de una tarea por partida: cada segundo avanza los cronometros y
 * cada 200 ms los retos, siempre solo de las partidas de las que esta instancia es dueña. Asi una
 * partida no depende de la instancia donde arranco: si esa muere, el reloj de otra la toma.
 */
@Component
public class RelojPartidas {

    private final TaskScheduler scheduler;
    private final MatchTimerService cronometros;
    private final HeroVillainChallengeService retos;

    public RelojPartidas(@Qualifier("gameplayScheduler") TaskScheduler scheduler,
                         MatchTimerService cronometros, HeroVillainChallengeService retos) {
        this.scheduler = scheduler;
        this.cronometros = cronometros;
        this.retos = retos;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void arrancar() {
        scheduler.scheduleAtFixedRate(cronometros::tickTodas, MatchTimerService.TICK_INTERVAL);
        scheduler.scheduleAtFixedRate(retos::tickTodos, HeroVillainChallengeService.BROADCAST_INTERVAL);
    }
}
