package co.eci.c15.gameplay.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.events.ChallengeFinishedEvent;
import co.eci.c15.common.events.EventosEntreServicios;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.gameplay.domain.MatchResult;
import co.eci.c15.gameplay.domain.MatchScoring;
import co.eci.c15.gameplay.domain.ProgresoEquipo;
import co.eci.c15.gameplay.domain.ProgresoRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * End of the match and its winner (HU-67.6). The match ends when time is up or as soon as a
 * team solves every puzzle of its map. Score = solved puzzles (HU-33.5) + challenges won
 * (HU-44.5); ties are broken by solved puzzles and otherwise it is a draw. Once it ends the map
 * stays frozen (see {@link #isFinished}).
 *
 * <p>Teams, challenges won and the result live in {@link ResultadosPartida}, so any instance can end
 * the match (the last puzzle may be solved anywhere); the result is saved once and only whoever saves
 * it announces it.
 */
@Service
public class MatchResultService {

    private static final Logger log = LoggerFactory.getLogger(MatchResultService.class);

    private final ProgresoPartidaUseCase progress;
    private final ProgresoRepository progressRepository;
    private final HeroVillainChallengeService challenges;
    private final MatchTimerService timers;
    private final MatchResultNotifier notifier;
    private final ResultadosPartida resultados;
    private final EventosEntreServicios entreServicios;

    public MatchResultService(ProgresoPartidaUseCase progress, ProgresoRepository progressRepository,
                              HeroVillainChallengeService challenges, MatchTimerService timers,
                              MatchResultNotifier notifier,
                              ResultadosPartida resultados, EventosEntreServicios entreServicios) {
        this.progress = progress;
        this.progressRepository = progressRepository;
        this.challenges = challenges;
        this.timers = timers;
        this.notifier = notifier;
        this.resultados = resultados;
        this.entreServicios = entreServicios;
    }

    public void matchStarted(String matchId, Map<String, List<String>> teams) {
        resultados.guardarEquipos(matchId, teams);
    }

    public void challengeFinished(ChallengeFinishedEvent event) {
        if (event.winnerTeamId() == null) return;
        resultados.sumarRetoGanado(event.matchId(), event.winnerTeamId());
    }

    /**
     * Counts the puzzle (idempotent, so it doesn't matter whether the progress listener ran
     * first) and ends the match if the team has now solved them all.
     */
    public void puzzleSolved(AcertijoResueltoEvent event) {
        if (resultados.equipos(event.matchId()).isEmpty()) return;
        progress.acertijoResuelto(event);
        boolean completed = progressRepository.findByMatchId(event.matchId()).stream()
                .anyMatch(p -> p.getEquipoId().equals(event.equipoId()) && p.estaCompleto());
        if (completed) finish(event.matchId(), MatchResult.Reason.ALL_PUZZLES_SOLVED);
    }

    public void timeUp(String matchId) {
        finish(matchId, MatchResult.Reason.TIME_UP);
    }

    public synchronized Optional<MatchResult> finish(String matchId, MatchResult.Reason reason) {
        Optional<MatchResult> existing = resultados.find(matchId);
        if (existing.isPresent()) return existing;
        Map<String, List<String>> members = resultados.equipos(matchId).orElse(null);
        if (members == null) return Optional.empty();
        List<String> teams = List.copyOf(members.keySet());

        // Closes a running challenge first so its winner is counted (publishes ChallengeFinishedEvent).
        challenges.matchOver(matchId);
        if (reason == MatchResult.Reason.ALL_PUZZLES_SOLVED) timers.stop(matchId);

        Map<String, Integer> puzzles = progressRepository.findByMatchId(matchId).stream()
                .collect(Collectors.toMap(ProgresoEquipo::getEquipoId, ProgresoEquipo::cantidadResueltos));
        MatchResult result = MatchScoring.rank(matchId, reason, teams, puzzles, resultados.retosGanados(matchId));
        // Si otra instancia lo termino al mismo tiempo, vale el suyo y ella lo anuncia
        if (!resultados.guardar(result)) return resultados.find(matchId);

        notifier.notify(result);
        EventoNegocio.de(log, "partida.terminada").con("matchId", matchId).con("motivo", reason)
                .con("equipoGanadorId", result.winnerTeamId()).info();
        Map<String, Integer> scores = new LinkedHashMap<>();
        result.standings().forEach(t -> scores.put(t.teamId(), t.score()));
        entreServicios.publicar(new MatchFinishedEvent(matchId, reason.name(), result.winnerTeamId(), scores, members));
        return Optional.of(result);
    }

    public boolean isFinished(String matchId) {
        return resultados.find(matchId).isPresent();
    }

    public Optional<MatchResult> find(String matchId) {
        return resultados.find(matchId);
    }
}
