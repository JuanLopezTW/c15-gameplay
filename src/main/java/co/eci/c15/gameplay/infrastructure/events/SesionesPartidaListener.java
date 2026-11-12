package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.SesionEvent;
import co.eci.c15.gameplay.application.DesconectarJugadorUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cuando el broker avisa (topic c15.sesion) que se cerro la conexion de un jugador en el mapa de una partida,
 * el jugador sale del mapa y se libera el acertijo que tuviera abierto. Las aperturas y renovaciones no
 * cambian nada aqui: el jugador ya entro al mapa al iniciar la partida.
 */
@Component
public class SesionesPartidaListener {

    private final DesconectarJugadorUseCase desconectar;

    public SesionesPartidaListener(DesconectarJugadorUseCase desconectar) {
        this.desconectar = desconectar;
    }

    @EventListener
    public void onSesion(SesionEvent sesion) {
        if (SesionEvent.PARTIDA.equals(sesion.ambito()) && !sesion.abierta()) {
            desconectar.ejecutar(sesion.id(), sesion.userId());
        }
    }
}
