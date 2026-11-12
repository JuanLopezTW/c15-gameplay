package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.domain.events.TimeUpEvent;
import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Schedules the match's challenge when it begins and closes it if time runs out (HU-44.5). */
@Component
public class ChallengeListener {

    private final HeroVillainChallengeService challenges;

    public ChallengeListener(HeroVillainChallengeService challenges) {
        this.challenges = challenges;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        challenges.schedule(event.matchId(), event.equipos().keySet(), event.duracion());
    }

    @EventListener
    public void onTimeUp(TimeUpEvent event) {
        challenges.matchOver(event.matchId());
    }
}
