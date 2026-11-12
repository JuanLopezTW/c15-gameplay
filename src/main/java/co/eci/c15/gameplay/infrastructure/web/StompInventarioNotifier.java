package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.InventarioDto;
import co.eci.c15.gameplay.application.InventarioNotifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompInventarioNotifier implements InventarioNotifier {

    private final SimpMessagingTemplate messaging;

    public StompInventarioNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notificar(InventarioDto inventario) {
        messaging.convertAndSend("/topic/partidas/" + inventario.matchId() + "/equipos/" + inventario.equipoId() + "/inventario", inventario);
    }
}
