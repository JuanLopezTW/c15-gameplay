package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.events.ChallengeFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.domain.events.TimeUpEvent;
import co.eci.c15.gameplay.application.MatchResultService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Feeds the match result (HU-67.6) with the match events. */
@Component
public class MatchResultListener {

    private final MatchResultService results;

    public MatchResultListener(MatchResultService results) {
        this.results = results;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        results.matchStarted(event.matchId(), event.equipos());
    }

    @EventListener
    public void onChallengeFinished(ChallengeFinishedEvent event) {
        results.challengeFinished(event);
    }

    @EventListener
    public void onAcertijoResuelto(AcertijoResueltoEvent event) {
        results.puzzleSolved(event);
    }

    @EventListener
    public void onTimeUp(TimeUpEvent event) {
        results.timeUp(event.matchId());
    }
}
