package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.TimerNotifier;
import co.eci.c15.gameplay.application.TimerState;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@Component
public class StompTimerNotifier implements TimerNotifier {

    private final SimpMessagingTemplate messaging;

    public StompTimerNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notify(TimerState state) {
        messaging.convertAndSend("/topic/partidas/" + state.matchId() + "/cronometro", state);
    }
}