package co.eci.c15.gameplay.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.gameplay.domain.events.ChallengeFinishedEvent;
import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.ChallengeResult;
import co.eci.c15.gameplay.domain.ChallengeTiming;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Lifecycle of the Hero vs Executioner challenge (HU-44.5): it fires once per match at a random
 * moment between 30% and 70% of the timer, lasts {@code c15.reto.duracion} (20s), broadcasts the
 * live scores and, when it ends, publishes {@link ChallengeFinishedEvent}. While it runs the map
 * is frozen (see {@link #isActive}).
 *
 * <p>The state lives in {@link RetosPartida} (shared between instances when Redis is on) and its
 * transitions happen once no matter who tries them. {@link #tickTodos} (called by {@code RelojPartidas}
 * every {@link #BROADCAST_INTERVAL}) starts, broadcasts and closes the challenges of the matches this
 * instance owns.
 */
@Service
public class HeroVillainChallengeService {

    private static final Logger log = LoggerFactory.getLogger(HeroVillainChallengeService.class);
    public static final Duration BROADCAST_INTERVAL = Duration.ofMillis(200);

    private final RetosPartida retos;
    private final DuenoPartidas duenos;
    private final Clock clock;
    private final RolesRepository roles;
    private final ChallengeNotifier notifier;
    private final ApplicationEventPublisher events;
    private final RandomGenerator random;
    private final Duration challengeDuration;

    @SuppressWarnings("java:S107") // dependencias de la HU: estado, reloj, roles, avisos, azar y duracion
    public HeroVillainChallengeService(RetosPartida retos,
                                       DuenoPartidas duenos,
                                       Clock clock,
                                       RolesRepository roles,
                                       ChallengeNotifier notifier,
                                       ApplicationEventPublisher events,
                                       @Qualifier("challengeRandom") RandomGenerator random,
                                       @Value("${c15.reto.duracion:20s}") Duration challengeDuration) {
        this.retos = retos;
        this.duenos = duenos;
        this.clock = clock;
        this.roles = roles;
        this.notifier = notifier;
        this.events = events;
        this.random = random;
        this.challengeDuration = challengeDuration;
    }

    /** Programs the match's only challenge at a random moment of the 30%-70% window of its duration. */
    public void schedule(String matchId, Collection<String> teamIds, Duration matchDuration) {
        Instant at = clock.instant().plus(ChallengeTiming.randomStart(matchDuration, random));
        retos.programar(matchId, teamIds.stream().sorted().toList(), at);
    }

    /** Starts the challenge right away (also used by the dev endpoint). */
    public boolean start(String matchId) {
        RetosPartida.Plan plan = retos.plan(matchId).orElse(null);
        if (plan == null || plan.estado() != RetosPartida.Estado.PROGRAMADO) return false;

        Map<String, Map<String, Rol>> rolesByTeam = new LinkedHashMap<>();
        plan.equipos().forEach(team -> roles.findByEquipo(matchId, team)
                .map(AsignacionRoles::roles)
                .ifPresent(r -> rolesByTeam.put(team, r)));
        if (rolesByTeam.size() < 2) {
            retos.cancelar(matchId);
            EventoNegocio.de(log, "reto.sin-roles").con("matchId", matchId).warn();
            return false;
        }

        Instant endsAt = clock.instant().plus(challengeDuration);
        if (!retos.iniciar(matchId, rolesByTeam, endsAt)) return false;
        Map<String, Long> ceros = new LinkedHashMap<>();
        rolesByTeam.keySet().stream().sorted().forEach(team -> ceros.put(team, 0L));
        notifier.notify(new ChallengeState(matchId, ChallengeState.STARTED, restantes(endsAt), ceros, null));
        EventoNegocio.de(log, "reto.iniciado").con("matchId", matchId).con("segundos", challengeDuration.toSeconds()).info();
        return true;
    }

    public void click(String matchId, String userId) {
        retos.click(matchId, userId, clock.instant());
    }

    /** While true the map is frozen: no moving, no puzzles, no picking up objects. */
    public boolean isActive(String matchId) {
        return retos.enCurso(matchId).isPresent();
    }

    public Optional<ChallengeState> find(String matchId) {
        return retos.enCurso(matchId).map(this::running);
    }

    public Optional<ChallengeResult> finish(String matchId) {
        Optional<ChallengeResult> cerrado = retos.terminar(matchId);
        cerrado.ifPresent(result -> {
            notifier.notify(new ChallengeState(matchId, ChallengeState.FINISHED, 0, result.scores(), result.winnerTeamId()));
            EventoNegocio.de(log, "reto.terminado").con("matchId", matchId).con("equipoGanadorId", result.winnerTeamId())
                    .con("puntajes", result.scores()).info();
            events.publishEvent(new ChallengeFinishedEvent(matchId, result.winnerTeamId(), result.scores()));
        });
        return cerrado;
    }

    /** The match is over: the pending challenge is dropped and a running one is closed. */
    public void matchOver(String matchId) {
        retos.cancelar(matchId);
        finish(matchId);
    }

    /** Un paso para cada reto de las partidas de las que esta instancia es dueña. */
    public void tickTodos() {
        List<String> activos;
        try {
            activos = retos.activos();
        } catch (RuntimeException e) {
            // Si el reloj deja escapar un error, el scheduler no lo vuelve a llamar nunca
            log.error("No se pudieron leer los retos activos", e);
            return;
        }
        for (String matchId : activos) {
            try {
                if (duenos.esMia(matchId)) tick(matchId);
            } catch (RuntimeException e) {
                log.error("Error en el reto de la partida {}", matchId, e);
            }
        }
    }

    /** Arranca el reto a su hora, manda el marcador mientras corre y lo cierra al cumplirse el tiempo. */
    void tick(String matchId) {
        RetosPartida.Plan plan = retos.plan(matchId).orElse(null);
        if (plan == null) return;
        Instant now = clock.instant();
        if (plan.estado() == RetosPartida.Estado.PROGRAMADO && !now.isBefore(plan.inicio())) {
            start(matchId);
        } else if (plan.estado() == RetosPartida.Estado.EN_CURSO) {
            RetosPartida.EnCurso enCurso = retos.enCurso(matchId).orElse(null);
            if (enCurso == null) return;
            if (!now.isBefore(enCurso.fin())) finish(matchId);
            else notifier.notify(running(enCurso));
        }
    }

    private ChallengeState running(RetosPartida.EnCurso enCurso) {
        return new ChallengeState(enCurso.matchId(), ChallengeState.RUNNING, restantes(enCurso.fin()),
                enCurso.puntajes(), null);
    }

    private long restantes(Instant fin) {
        long ms = Duration.between(clock.instant(), fin).toMillis();
        return ms <= 0 ? 0 : (ms + 999) / 1000;
    }
}
