package co.eci.c15.gameplay.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Saca al jugador del mapa y avisa a su equipo. Es idempotente. */
@Service
public class DesconectarJugadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(DesconectarJugadorUseCase.class);

    private final JugadorEnMapaRepository jugadores;
    private final PosicionNotifier notifier;

    public DesconectarJugadorUseCase(JugadorEnMapaRepository jugadores, PosicionNotifier notifier) {
        this.jugadores = jugadores;
        this.notifier = notifier;
    }

    public void ejecutar(String matchId, String userId) {
        jugadores.remove(matchId, userId).ifPresent(jugador -> {
            notifier.notificar(matchId, jugador.getEquipoId(), PosicionJugadorDto.desconectado(jugador));
            EventoNegocio.de(log, "jugador.salio_partida").con("matchId", matchId).con("equipoId", jugador.getEquipoId())
                    .con("userId", userId).info();
        });
    }
}
