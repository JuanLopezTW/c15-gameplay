package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ProgresoNotifier;
import co.eci.c15.gameplay.application.ProgresoPartidaDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompProgresoNotifier implements ProgresoNotifier {

    private final SimpMessagingTemplate messaging;

    public StompProgresoNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notificar(ProgresoPartidaDto progreso) {
        messaging.convertAndSend("/topic/partidas/" + progreso.matchId() + "/progreso", progreso);
    }
}
