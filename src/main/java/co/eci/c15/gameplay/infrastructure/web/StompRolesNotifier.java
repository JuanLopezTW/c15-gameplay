package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.AsignacionRolesDto;
import co.eci.c15.gameplay.application.RolesNotifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompRolesNotifier implements RolesNotifier {

    private final SimpMessagingTemplate messaging;

    public StompRolesNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notificar(AsignacionRolesDto asignacion) {
        messaging.convertAndSend("/topic/partidas/" + asignacion.matchId() + "/equipos/" + asignacion.equipoId() + "/roles", asignacion);
    }
}
