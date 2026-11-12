package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.AsignarRolesUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RolesPartidaListener {

    private final AsignarRolesUseCase asignarRoles;

    public RolesPartidaListener(AsignarRolesUseCase asignarRoles) {
        this.asignarRoles = asignarRoles;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        asignarRoles.ejecutar(event.matchId(), event.equipos());
    }
}
