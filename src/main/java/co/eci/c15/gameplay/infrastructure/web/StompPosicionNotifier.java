package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.PosicionJugadorDto;
import co.eci.c15.gameplay.application.PosicionNotifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompPosicionNotifier implements PosicionNotifier {

    private final SimpMessagingTemplate messaging;

    public StompPosicionNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notificar(String matchId, String equipoId, PosicionJugadorDto posicion) {
        messaging.convertAndSend("/topic/partidas/" + matchId + "/equipos/" + equipoId + "/posiciones", posicion);
    }
}
