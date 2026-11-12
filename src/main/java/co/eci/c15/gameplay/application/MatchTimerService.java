package co.eci.c15.gameplay.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.gameplay.domain.events.TimeUpEvent;
import co.eci.c15.gameplay.domain.MatchTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cronometro de cada partida (HU-34.5). Solo se guarda cuando empezo y cuanto dura: cualquier instancia
 * calcula lo que falta. Cada segundo {@link #tickTodas} (lo llama {@code RelojPartidas}) avisa el tiempo de
 * las partidas de las que esta instancia es dueña y, al agotarse, publica {@link TimeUpEvent} una sola vez.
 */
@Service
public class MatchTimerService {

    private static final Logger log = LoggerFactory.getLogger(MatchTimerService.class);
    public static final Duration TICK_INTERVAL = Duration.ofSeconds(1);

    private final CronometrosPartida cronometros;
    private final DuenoPartidas duenos;
    private final Clock clock;
    private final TimerNotifier notifier;
    private final ApplicationEventPublisher events;

    public MatchTimerService(CronometrosPartida cronometros,
                             DuenoPartidas duenos,
                             Clock clock,
                             TimerNotifier notifier,
                             ApplicationEventPublisher events) {
        this.cronometros = cronometros;
        this.duenos = duenos;
        this.clock = clock;
        this.notifier = notifier;
        this.events = events;
    }

    public MatchTimer start(String matchId, Duration duration) {
        MatchTimer created = new MatchTimer(matchId, clock.instant(), duration);
        if (!cronometros.crear(created)) {
            return cronometros.find(matchId).orElse(created);
        }
        notifier.notify(stateOf(created, clock.instant()));
        EventoNegocio.de(log, "cronometro.iniciado").con("matchId", matchId).con("segundos", duration.toSeconds()).info();
        return created;
    }

    /** Stops the timer without publishing {@link TimeUpEvent} (the match ended early). */
    public boolean stop(String matchId) {
        return cronometros.quitar(matchId);
    }

    public Optional<TimerState> find(String matchId) {
        return cronometros.find(matchId).map(timer -> stateOf(timer, clock.instant()));
    }

    /** Un paso del reloj para cada partida de la que esta instancia es dueña. */
    public void tickTodas() {
        List<String> activos;
        try {
            activos = cronometros.activos();
        } catch (RuntimeException e) {
            // Si el reloj deja escapar un error, el scheduler no lo vuelve a llamar nunca
            log.error("No se pudieron leer los cronometros activos", e);
            return;
        }
        for (String matchId : activos) {
            try {
                if (duenos.esMia(matchId)) tick(matchId);
            } catch (RuntimeException e) {
                log.error("Error en el tick del cronometro de la partida {}", matchId, e);
            }
        }
    }

    void tick(String matchId) {
        MatchTimer timer = cronometros.find(matchId).orElse(null);
        if (timer == null) {
            return;
        }
        Instant now = clock.instant();
        notifier.notify(stateOf(timer, now));
        if (timer.isTimeUp(now) && cronometros.quitar(matchId)) {
            EventoNegocio.de(log, "cronometro.agotado").con("matchId", matchId).info();
            events.publishEvent(new TimeUpEvent(matchId, now));
        }
    }

    private TimerState stateOf(MatchTimer timer, Instant now) {
        return new TimerState(timer.getMatchId(), timer.remainingSeconds(now), timer.isTimeUp(now));
    }
}
