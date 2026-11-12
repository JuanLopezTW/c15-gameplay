package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ChallengeNotifier;
import co.eci.c15.gameplay.application.ChallengeState;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompChallengeNotifier implements ChallengeNotifier {

    private final SimpMessagingTemplate messaging;

    public StompChallengeNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notify(ChallengeState state) {
        messaging.convertAndSend("/topic/partidas/" + state.matchId() + "/reto", state);
    }
}
