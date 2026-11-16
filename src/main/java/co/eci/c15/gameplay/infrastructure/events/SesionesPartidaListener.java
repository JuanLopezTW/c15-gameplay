package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.SesionEvent;
import co.eci.c15.gameplay.application.DesconectarJugadorUseCase;
import co.eci.c15.gameplay.application.SesionesEnMapa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Las conexiones de los jugadores en el mapa (topic c15.sesion, del broker). Cuando una se cierra, el jugador no
 * sale en el acto: tiene {@code gracia} para volver (recargar la pagina o un corte de red). Si en ese tiempo abrio
 * otra conexion, sigue en la partida donde estaba; si no, sale del mapa y se libera el acertijo que tuviera abierto.
 * La gracia es mayor que la renovacion del broker (30 s): un jugador con dos pestanas no queda fuera por esa carrera.
 */
@Component
public class SesionesPartidaListener {

    private static final Logger log = LoggerFactory.getLogger(SesionesPartidaListener.class);

    private final DesconectarJugadorUseCase desconectar;
    private final SesionesEnMapa sesiones;
    private final TaskScheduler scheduler;
    private final Duration gracia;

    public SesionesPartidaListener(DesconectarJugadorUseCase desconectar, SesionesEnMapa sesiones,
                                   @Qualifier("gameplayScheduler") TaskScheduler scheduler,
                                   @Value("${c15.partida.gracia-desconexion:40s}") Duration gracia) {
        this.desconectar = desconectar;
        this.sesiones = sesiones;
        this.scheduler = scheduler;
        this.gracia = gracia;
    }

    @EventListener
    public void onSesion(SesionEvent sesion) {
        if (!SesionEvent.PARTIDA.equals(sesion.ambito())) return;
        if (sesion.estaAbierta()) {
            sesiones.abierta(sesion.id(), sesion.userId(), sesion.sessionId());
            return;
        }
        scheduler.schedule(() -> salirSiNoVolvio(sesion), Instant.now().plus(gracia));
    }

    /** Pasada la gracia: si no volvio con otra conexion, sale de la partida. Un error no tumba el scheduler. */
    void salirSiNoVolvio(SesionEvent cerrada) {
        try {
            if (sesiones.esLaUltima(cerrada.id(), cerrada.userId(), cerrada.sessionId())) {
                desconectar.ejecutar(cerrada.id(), cerrada.userId());
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo revisar la salida de un jugador de la partida {}", cerrada.id(), e);
        }
    }
}
