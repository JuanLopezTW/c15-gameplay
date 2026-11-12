package co.eci.c15.gameplay.application;

import co.eci.c15.common.events.EventosEntreServicios;
import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.events.ChallengeFinishedEvent;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.gameplay.domain.MatchResult;
import co.eci.c15.gameplay.domain.ProgresoRepository;
import co.eci.c15.gameplay.infrastructure.estado.TiempoPartidaEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.ProgresoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchResultServiceTest {

    private static final Map<String, List<String>> EQUIPOS = Map.of("A", List.of("ana"), "B", List.of("beto"));

    private ProgresoRepository progressRepository;
    private ProgresoPartidaUseCase progress;
    private HeroVillainChallengeService challenges;
    private MatchTimerService timers;
    private MatchResultNotifier notifier;
    private EventosEntreServicios events;
    private MatchResultService service;

    @BeforeEach
    void setUp() {
        progressRepository = new ProgresoRepositoryEnMemoria();
        progress = mock(ProgresoPartidaUseCase.class);
        // Same effect as the real use case: counts the puzzle for the team (3 puzzles per map).
        doAnswer(inv -> {
            AcertijoResueltoEvent e = inv.getArgument(0);
            progressRepository.obtener(e.matchId(), e.equipoId(), 3).registrarResuelto(e.componenteMapaId());
            return null;
        }).when(progress).acertijoResuelto(any());
        challenges = mock(HeroVillainChallengeService.class);
        timers = mock(MatchTimerService.class);
        notifier = mock(MatchResultNotifier.class);
        events = mock(EventosEntreServicios.class);
        service = new MatchResultService(progress, progressRepository, challenges, timers, notifier,
                new TiempoPartidaEnMemoria().resultadosEnMemoria(), events);
        service.matchStarted("m1", EQUIPOS);
        progressRepository.obtener("m1", "A", 3);
        progressRepository.obtener("m1", "B", 3);
    }

    private void solve(String team, String puzzle) {
        service.puzzleSolved(new AcertijoResueltoEvent("m1", team, puzzle, "user"));
    }

    @Test
    void whenTimeIsUpTheTeamWithMostPointsWins() {
        solve("A", "acertijo-1");
        solve("B", "acertijo-1");
        solve("B", "acertijo-2");

        service.timeUp("m1");

        MatchResult result = service.find("m1").orElseThrow();
        assertEquals("B", result.winnerTeamId());
        assertEquals(MatchResult.Reason.TIME_UP, result.reason());
        assertTrue(service.isFinished("m1"));
        verify(challenges).matchOver("m1");
        verify(timers, never()).stop(any());
        verify(notifier).notify(result);
        verify(events).publicar(new MatchFinishedEvent("m1", "TIME_UP", "B", Map.of("B", 2, "A", 1), EQUIPOS));
    }

    @Test
    void theChallengeWinnerGetsAPuzzleWorthOfPoints() {
        solve("A", "acertijo-1");
        solve("B", "acertijo-1");
        service.challengeFinished(new ChallengeFinishedEvent("m1", "A", Map.of("A", 10L, "B", 3L)));

        service.timeUp("m1");

        assertEquals("A", service.find("m1").orElseThrow().winnerTeamId());
    }

    @Test
    void aTiedChallengeGivesNothing() {
        service.challengeFinished(new ChallengeFinishedEvent("m1", null, Map.of("A", 5L, "B", 5L)));
        service.timeUp("m1");
        assertTrue(service.find("m1").orElseThrow().isDraw());
    }

    @Test
    void sameScoreAndPuzzlesIsADraw() {
        solve("A", "acertijo-1");
        solve("B", "acertijo-2");

        service.timeUp("m1");

        MatchResult result = service.find("m1").orElseThrow();
        assertTrue(result.isDraw());
        assertEquals(List.of("A", "B"), result.tiedTeamIds());
        verify(events).publicar(new MatchFinishedEvent("m1", "TIME_UP", null, Map.of("A", 1, "B", 1), EQUIPOS));
    }

    @Test
    void solvingEveryPuzzleEndsTheMatchEarlyAndStopsTheTimer() {
        solve("A", "acertijo-1");
        solve("A", "acertijo-2");
        assertFalse(service.isFinished("m1"));

        solve("A", "acertijo-3");

        MatchResult result = service.find("m1").orElseThrow();
        assertEquals(MatchResult.Reason.ALL_PUZZLES_SOLVED, result.reason());
        assertEquals("A", result.winnerTeamId());
        verify(timers).stop("m1");
        verify(challenges).matchOver("m1");
    }

    @Test
    void finishingFirstWithTheSameScoreIsADraw() {
        solve("B", "acertijo-1");
        solve("B", "acertijo-2");
        service.challengeFinished(new ChallengeFinishedEvent("m1", "B", Map.of("A", 0L, "B", 9L)));
        solve("A", "acertijo-1");
        solve("A", "acertijo-2");
        solve("A", "acertijo-3");

        MatchResult result = service.find("m1").orElseThrow();
        // A: 3 puzzles = 3; B: 2 puzzles + challenge = 3 -> same score: draw
        assertEquals(MatchResult.Reason.ALL_PUZZLES_SOLVED, result.reason());
        assertTrue(result.isDraw());
        assertNull(result.winnerTeamId());
        assertEquals(3, result.standings().get(1).score());
    }

    @Test
    void theMatchEndsOnlyOnce() {
        service.timeUp("m1");
        service.timeUp("m1");
        solve("A", "acertijo-1");

        verify(notifier, times(1)).notify(any());
        verify(events, times(1)).publicar(any(MatchFinishedEvent.class));
        assertEquals(0, service.find("m1").orElseThrow().standings().get(0).score());
    }

    @Test
    void unknownMatchHasNoResult() {
        assertTrue(service.finish("no-existe", MatchResult.Reason.TIME_UP).isEmpty());
        service.puzzleSolved(new AcertijoResueltoEvent("no-existe", "A", "acertijo-1", "u"));
        verifyNoInteractions(notifier, events, challenges);
        verify(progress, never()).acertijoResuelto(any());
    }
}
