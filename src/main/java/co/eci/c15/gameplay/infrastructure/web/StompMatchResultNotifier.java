package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.MatchResultNotifier;
import co.eci.c15.gameplay.domain.MatchResult;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompMatchResultNotifier implements MatchResultNotifier {

    private final SimpMessagingTemplate messaging;

    public StompMatchResultNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notify(MatchResult result) {
        messaging.convertAndSend("/topic/partidas/" + result.matchId() + "/resultado", result);
    }
}
